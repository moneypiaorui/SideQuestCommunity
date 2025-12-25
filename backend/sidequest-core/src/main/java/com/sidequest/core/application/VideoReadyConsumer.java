package com.sidequest.core.application;

import com.sidequest.core.infrastructure.PostDO;
import com.sidequest.core.infrastructure.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoReadyConsumer {

    private final PostMapper postMapper;

    @KafkaListener(topics = "video-ready-topic", groupId = "core-group")
    public void onVideoReady(@Header(KafkaHeaders.RECEIVED_KEY) String mediaIdStr, @Payload String payload) {
        log.info("Received video ready event for mediaId: {}, payload: {}", mediaIdStr, payload);
        try {
            Long mediaId = Long.parseLong(mediaIdStr);
            
            // 简单解析 JSON
            String hlsUrl = payload.contains("videoUrl\":\"") ? payload.split("videoUrl\":\"")[1].split("\"")[0] : "";
            String coverUrl = payload.contains("videoCoverUrl\":\"") ? payload.split("videoCoverUrl\":\"")[1].split("\"")[0] : "";

            // 更新所有关联该 mediaId 的帖子视频地址为 HLS 地址，并同步封面图
            postMapper.update(null, new LambdaUpdateWrapper<PostDO>()
                    .eq(PostDO::getMediaId, mediaId)
                    .set(PostDO::getVideoUrl, hlsUrl)
                    .set(!coverUrl.isEmpty(), PostDO::getVideoCoverUrl, coverUrl));
            
            log.info("Successfully updated post video and cover URL for mediaId: {}", mediaId);
        } catch (Exception e) {
            log.error("Failed to update post media for mediaId: {}", mediaIdStr, e);
        }
    }
}

