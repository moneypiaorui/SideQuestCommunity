package com.sidequest.media.application;

import com.sidequest.media.domain.Media;
import com.sidequest.media.infrastructure.MediaDO;
import com.sidequest.media.infrastructure.mapper.MediaMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MediaMapper mediaMapper;
    
    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;
    
    @Value("${minio.accessKey:minioadmin}")
    private String accessKey;
    
    @Value("${minio.secretKey:minioadmin}")
    private String secretKey;
    
    @Value("${minio.bucket:sidequest}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public List<MediaDO> getAuthorMedia(Long authorId) {
        return mediaMapper.selectList(new LambdaQueryWrapper<MediaDO>().eq(MediaDO::getAuthorId, authorId));
    }

    public String getUploadUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(fileName)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating upload URL", e);
            throw new RuntimeException("Could not generate upload URL");
        }
    }

    public void processVideo(Long mediaId) {
        // 更新状态为处理中
        updateStatus(mediaId, MediaDO.STATUS_PROCESSING);
        // 发送 Kafka 消息触发视频转码服务（异步处理）
        kafkaTemplate.send("video-process-topic", mediaId.toString(), "Start processing video: " + mediaId);
        log.info("Sent video processing request for mediaId: {}", mediaId);
    }

    public Integer getStatus(Long mediaId) {
        MediaDO mediaDO = mediaMapper.selectById(mediaId);
        return mediaDO != null ? mediaDO.getStatus() : null;
    }

    public void updateStatus(Long mediaId, Integer status) {
        MediaDO mediaDO = mediaMapper.selectById(mediaId);
        if (mediaDO != null) {
            mediaDO.setStatus(status);
            mediaMapper.updateById(mediaDO);
            log.info("Successfully updated media status for {}: {}", mediaId, status);
        }
    }
}

