package com.sidequest.media.application;

import com.sidequest.media.domain.Media;
import com.sidequest.media.infrastructure.DanmakuDO;
import com.sidequest.media.infrastructure.MediaDO;
import com.sidequest.media.infrastructure.mapper.DanmakuMapper;
import com.sidequest.media.infrastructure.mapper.MediaMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MediaMapper mediaMapper;
    private final DanmakuMapper danmakuMapper;
    
    @Value("${minio.endpoint:http://minio:9000}")
    private String endpoint;
    
    @Value("${minio.accessKey:minioadmin}")
    private String accessKey;
    
    @Value("${minio.secretKey:minioadmin}")
    private String secretKey;
    
    @Value("${minio.bucket:sidequest}")
    private String bucket;

    @Value("${minio.publicEndpoint:}")
    private String publicEndpoint;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            // 检查并创建 Bucket
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Successfully created minio bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Minio client or bucket", e);
        }
    }

    public List<MediaDO> getAuthorMedia(Long authorId) {
        return mediaMapper.selectList(new LambdaQueryWrapper<MediaDO>().eq(MediaDO::getAuthorId, authorId));
    }

    public String getUploadUrl(String fileName) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(fileName)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );

            // 如果配置了公网访问地址，则替换生成的内部地址
            if (publicEndpoint != null && !publicEndpoint.isBlank()) {
                url = url.replace(endpoint, publicEndpoint);
            }

            return url;
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

    public void saveDanmaku(DanmakuDO danmakuDO) {
        danmakuDO.setCreateTime(LocalDateTime.now());
        danmakuMapper.insert(danmakuDO);
        log.info("Successfully saved danmaku to DB for video {}: {}", danmakuDO.getVideoId(), danmakuDO.getContent());
    }
}

