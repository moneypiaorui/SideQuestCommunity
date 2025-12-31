package com.sidequest.media.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.media.application.MediaService;
import com.sidequest.media.domain.Danmaku;
import com.sidequest.media.infrastructure.DanmakuDO;
import com.sidequest.media.infrastructure.MediaDO;
import com.sidequest.media.interfaces.dto.DanmakuRequest;
import com.sidequest.media.interfaces.dto.DanmakuVO;
import com.sidequest.media.interfaces.dto.MediaCompleteRequest;
import com.sidequest.media.interfaces.dto.MediaRegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/register")
    public Result<MediaDO> registerMedia(@Valid @RequestBody MediaRegisterRequest request) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            return Result.error(401, "Unauthorized");
        }
        Long userId = Long.parseLong(userIdStr);

        MediaDO mediaDO = new MediaDO();
        BeanUtils.copyProperties(request, mediaDO);
        mediaDO.setAuthorId(userId);

        return Result.success(mediaService.registerMedia(mediaDO));
    }

    @PostMapping("/complete")
    public Result<MediaDO> completeUpload(@Valid @RequestBody MediaCompleteRequest request) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            return Result.error(401, "Unauthorized");
        }
        Long userId = Long.parseLong(userIdStr);

        MediaDO mediaDO = new MediaDO();
        BeanUtils.copyProperties(request, mediaDO);
        mediaDO.setAuthorId(userId);

        return Result.success(mediaService.completeUpload(mediaDO));
    }

    @GetMapping("/list")
    public Result<List<MediaDO>> getMyMedia(@RequestParam Long authorId) {
        return Result.success(mediaService.getAuthorMedia(authorId));
    }

    @GetMapping("/upload-url")
    public Result<String> getUploadUrl(@RequestParam String fileName) {
        return Result.success(mediaService.getUploadUrl(fileName));
    }

    @GetMapping("/status/{id}")
    public Result<Integer> getStatus(@PathVariable Long id) {
        return Result.success(mediaService.getStatus(id));
    }

    @GetMapping("/{id}")
    public Result<MediaDO> getMediaDetail(@PathVariable Long id) {
        MediaDO mediaDO = mediaService.getMediaById(id);
        if (mediaDO == null) {
            return Result.error(404, "Media not found");
        }
        return Result.success(mediaDO);
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteMedia(@PathVariable Long id) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            return Result.error(401, "Unauthorized");
        }
        Long userId = Long.parseLong(userIdStr);
        MediaDO mediaDO = mediaService.getMediaById(id);
        if (mediaDO == null) {
            return Result.error(404, "Media not found");
        }
        if (!userId.equals(mediaDO.getAuthorId())) {
            return Result.forbidden();
        }
        mediaService.deleteMedia(mediaDO);
        return Result.success("Media deleted");
    }

    @PostMapping("/danmaku")
    public Result<String> sendDanmaku(@Valid @RequestBody DanmakuRequest request) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            return Result.error(401, "Unauthorized");
        }
        Long userId = Long.parseLong(userIdStr);

        DanmakuDO danmakuDO = new DanmakuDO();
        BeanUtils.copyProperties(request, danmakuDO);
        danmakuDO.setUserId(userId);

        // 1. 持久化到数据库
        mediaService.saveDanmaku(danmakuDO);

        // 2. 存储到 Redis ZSet，Score 为视频偏移时间，用于快速检索
        String key = "danmaku:" + request.getVideoId();
        // 构造领域模型用于缓存
        Danmaku danmaku = Danmaku.builder()
                .id(danmakuDO.getId())
                .videoId(danmakuDO.getVideoId())
                .userId(danmakuDO.getUserId())
                .content(danmakuDO.getContent())
                .timeOffsetMs(danmakuDO.getTimeOffsetMs())
                .color(danmakuDO.getColor())
                .createTime(danmakuDO.getCreateTime())
                .build();
        
        redisTemplate.opsForZSet().add(key, danmaku, danmaku.getTimeOffsetMs());
        
        return Result.success("Danmaku sent");
    }

    @GetMapping("/danmaku")
    public Result<List<DanmakuVO>> getDanmaku(@RequestParam Long videoId, @RequestParam Long fromMs, @RequestParam Long toMs) {
        String key = "danmaku:" + videoId;
        List<Object> range = List.copyOf(redisTemplate.opsForZSet().rangeByScore(key, fromMs, toMs));
        if (range.isEmpty()) {
            List<DanmakuDO> dbDanmakus = mediaService.getDanmakuRange(videoId, fromMs, toMs);
            if (!dbDanmakus.isEmpty()) {
                for (DanmakuDO danmakuDO : dbDanmakus) {
                    Danmaku danmaku = Danmaku.builder()
                            .id(danmakuDO.getId())
                            .videoId(danmakuDO.getVideoId())
                            .userId(danmakuDO.getUserId())
                            .content(danmakuDO.getContent())
                            .timeOffsetMs(danmakuDO.getTimeOffsetMs())
                            .color(danmakuDO.getColor())
                            .createTime(danmakuDO.getCreateTime())
                            .build();
                    redisTemplate.opsForZSet().add(key, danmaku, danmaku.getTimeOffsetMs());
                }
                range = List.copyOf(redisTemplate.opsForZSet().rangeByScore(key, fromMs, toMs));
            }
        }
        List<DanmakuVO> vos = range.stream().map(obj -> {
            Danmaku danmaku = (Danmaku) obj;
            DanmakuVO vo = new DanmakuVO();
            BeanUtils.copyProperties(danmaku, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }

    @DeleteMapping("/danmaku/{id}")
    public Result<String> deleteDanmaku(@PathVariable Long id) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            return Result.error(401, "Unauthorized");
        }
        Long userId = Long.parseLong(userIdStr);
        DanmakuDO danmakuDO = mediaService.deleteDanmaku(id, userId);
        if (danmakuDO == null) {
            return Result.error(404, "Danmaku not found");
        }
        String key = "danmaku:" + danmakuDO.getVideoId();
        List<Object> range = List.copyOf(redisTemplate.opsForZSet().rangeByScore(key, danmakuDO.getTimeOffsetMs(), danmakuDO.getTimeOffsetMs()));
        for (Object obj : range) {
            if (obj instanceof Danmaku danmaku && id.equals(danmaku.getId())) {
                redisTemplate.opsForZSet().remove(key, obj);
            }
        }
        return Result.success("Danmaku deleted");
    }
}

