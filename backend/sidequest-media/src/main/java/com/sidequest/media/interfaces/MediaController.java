package com.sidequest.media.interfaces;

import com.sidequest.common.Result;
import com.sidequest.media.application.MediaService;
import com.sidequest.media.domain.Danmaku;
import com.sidequest.media.infrastructure.MediaDO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;
    private final RedisTemplate<String, Object> redisTemplate;

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

    @PostMapping("/danmaku")
    public Result<String> sendDanmaku(@RequestBody Danmaku danmaku) {
        // 使用 Redis ZSet 存储弹幕，Score 为视频偏移时间
        String key = "danmaku:" + danmaku.getVideoId();
        redisTemplate.opsForZSet().add(key, danmaku, danmaku.getTimeOffsetMs());
        return Result.success("Danmaku sent");
    }

    @GetMapping("/danmaku")
    public Result<List<Object>> getDanmaku(@RequestParam Long videoId, @RequestParam Long fromMs, @RequestParam Long toMs) {
        String key = "danmaku:" + videoId;
        return Result.success(List.copyOf(redisTemplate.opsForZSet().rangeByScore(key, fromMs, toMs)));
    }
}

