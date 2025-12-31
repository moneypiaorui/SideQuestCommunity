package com.sidequest.moderation.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidequest.moderation.infrastructure.ModerationCaseDO;
import com.sidequest.moderation.infrastructure.ReportDO;
import com.sidequest.moderation.infrastructure.SensitiveWordDO;
import com.sidequest.moderation.infrastructure.mapper.ModerationCaseMapper;
import com.sidequest.moderation.infrastructure.mapper.ReportMapper;
import com.sidequest.moderation.infrastructure.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private static final String CACHE_SENSITIVE_WORDS = "sensitive:words:all";
    private static final String CACHE_RESULT_PREFIX = "moderation:result:";

    private final SensitiveWordMapper sensitiveWordMapper;
    private final ModerationCaseMapper moderationCaseMapper;
    private final ReportMapper reportMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public boolean checkText(String text) {
        return checkContent("text", text, null).result;
    }

    public boolean checkImage(String url) {
        return checkContent("image", null, url).result;
    }

    public boolean checkVideo(String url) {
        return checkContent("video", null, url).result;
    }

    public List<SensitiveWordDO> listSensitiveWords() {
        return sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWordDO>()
                .eq(SensitiveWordDO::getStatus, 1)
                .orderByAsc(SensitiveWordDO::getId));
    }

    @Transactional
    public SensitiveWordDO addSensitiveWord(String word, String level) {
        SensitiveWordDO sw = new SensitiveWordDO();
        sw.setWord(word);
        sw.setLevel(level == null || level.isBlank() ? "S1" : level);
        sw.setStatus(1);
        sw.setCreateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(sw);
        refreshSensitiveWordCache();
        return sw;
    }

    @Transactional
    public void deleteSensitiveWord(Long id) {
        sensitiveWordMapper.deleteById(id);
        refreshSensitiveWordCache();
    }

    public List<ModerationCaseDO> listCases(Integer status) {
        LambdaQueryWrapper<ModerationCaseDO> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(ModerationCaseDO::getStatus, status);
        }
        return moderationCaseMapper.selectList(query.orderByDesc(ModerationCaseDO::getId));
    }

    @Transactional
    public ModerationCaseDO handleCase(Long id, String result, String reason, Long operatorId) {
        ModerationCaseDO mc = moderationCaseMapper.selectById(id);
        if (mc == null) {
            return null;
        }
        mc.setResult(result);
        mc.setReason(reason);
        mc.setStatus(1);
        mc.setOperatorId(operatorId);
        mc.setUpdateTime(LocalDateTime.now());
        moderationCaseMapper.updateById(mc);
        sendModerationEvent(mc);
        return mc;
    }

    @Transactional
    public ReportDO createReport(ReportDO reportDO) {
        reportDO.setStatus(0);
        reportDO.setCreateTime(LocalDateTime.now());
        reportMapper.insert(reportDO);
        return reportDO;
    }

    public List<ReportDO> listReports(Integer status) {
        LambdaQueryWrapper<ReportDO> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(ReportDO::getStatus, status);
        }
        return reportMapper.selectList(query.orderByDesc(ReportDO::getId));
    }

    @Transactional
    public ReportDO handleReport(Long id, String handleResult, Integer status, Long handlerId) {
        ReportDO report = reportMapper.selectById(id);
        if (report == null) {
            return null;
        }
        report.setHandleResult(handleResult);
        report.setStatus(status);
        report.setHandlerId(handlerId);
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);
        return report;
    }

    private ModerationOutcome checkContent(String type, String text, String url) {
        String content = text != null ? text : url;
        if (content == null || content.isBlank()) {
            return new ModerationOutcome(true, "S0", null);
        }

        String cacheKey = CACHE_RESULT_PREFIX + sha256(type + ":" + content);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            String[] parts = cached.split("\\|", 3);
            boolean pass = Boolean.parseBoolean(parts[0]);
            String level = parts.length > 1 ? parts[1] : "S0";
            String reason = parts.length > 2 ? parts[2] : null;
            saveCase(type, text, url, pass, level, reason);
            return new ModerationOutcome(pass, level, reason);
        }

        List<SensitiveWordDO> words = loadSensitiveWords();
        if (words.isEmpty()) {
            saveCase(type, text, url, true, "S0", null);
            return new ModerationOutcome(true, "S0", null);
        }

        String matchedWord = null;
        String maxLevel = "S0";
        for (SensitiveWordDO word : words) {
            if (word.getWord() == null || word.getWord().isBlank()) {
                continue;
            }
            Pattern p = Pattern.compile(Pattern.quote(word.getWord()), Pattern.CASE_INSENSITIVE);
            if (p.matcher(content).find()) {
                matchedWord = word.getWord();
                if (compareLevel(word.getLevel(), maxLevel) > 0) {
                    maxLevel = word.getLevel();
                }
            }
        }

        boolean pass = matchedWord == null || "S0".equalsIgnoreCase(maxLevel);
        String reason = matchedWord == null ? null : "Hit sensitive word: " + matchedWord;
        stringRedisTemplate.opsForValue().set(cacheKey, pass + "|" + maxLevel + "|" + (reason == null ? "" : reason));
        saveCase(type, text, url, pass, maxLevel, reason);
        return new ModerationOutcome(pass, maxLevel, reason);
    }

    private void saveCase(String type, String text, String url, boolean pass, String level, String reason) {
        ModerationCaseDO mc = new ModerationCaseDO();
        mc.setContentType(type);
        mc.setContent(text);
        mc.setContentUrl(url);
        mc.setLevel(level);
        mc.setResult(pass ? "PASS" : "BLOCK");
        mc.setReason(reason);
        mc.setStatus(0);
        mc.setCreateTime(LocalDateTime.now());
        moderationCaseMapper.insert(mc);
        sendModerationEvent(mc);
    }

    private void sendModerationEvent(ModerationCaseDO mc) {
        try {
            String payload = objectMapper.writeValueAsString(mc);
            kafkaTemplate.send("moderation.result", mc.getId().toString(), payload);
        } catch (Exception ignored) {
        }
    }

    private List<SensitiveWordDO> loadSensitiveWords() {
        String cached = stringRedisTemplate.opsForValue().get(CACHE_SENSITIVE_WORDS);
        if (cached != null && !cached.isBlank()) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<SensitiveWordDO>>() {});
            } catch (Exception ignored) {
            }
        }

        List<SensitiveWordDO> words = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWordDO>()
                .eq(SensitiveWordDO::getStatus, 1));
        try {
            stringRedisTemplate.opsForValue().set(CACHE_SENSITIVE_WORDS, objectMapper.writeValueAsString(words));
        } catch (Exception ignored) {
        }
        return words;
    }

    private void refreshSensitiveWordCache() {
        List<SensitiveWordDO> words = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWordDO>()
                .eq(SensitiveWordDO::getStatus, 1));
        try {
            stringRedisTemplate.opsForValue().set(CACHE_SENSITIVE_WORDS, objectMapper.writeValueAsString(words));
        } catch (Exception ignored) {
        }
    }

    private int compareLevel(String level, String current) {
        int lv = levelToScore(level);
        int cv = levelToScore(current);
        return Integer.compare(lv, cv);
    }

    private int levelToScore(String level) {
        if (level == null) {
            return 0;
        }
        return switch (level.toUpperCase()) {
            case "S2" -> 2;
            case "S1" -> 1;
            default -> 0;
        };
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private static class ModerationOutcome {
        private final boolean result;
        private final String level;
        private final String reason;

        private ModerationOutcome(boolean result, String level, String reason) {
            this.result = result;
            this.level = level;
            this.reason = reason;
        }
    }
}
