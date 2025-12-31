package com.sidequest.media.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidequest.media.infrastructure.DanmakuDO;
import com.sidequest.media.infrastructure.MediaDO;
import com.sidequest.media.infrastructure.mapper.DanmakuMapper;
import com.sidequest.media.infrastructure.mapper.MediaMapper;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MediaMapper mediaMapper;
    private final DanmakuMapper danmakuMapper;
    private final ObjectMapper objectMapper;
    
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

            // 设置存储桶策略为公共只读，以便前端直接访问图片
            String policy = "{\n" +
                    "  \"Version\": \"2012-10-17\",\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
                    "      \"Action\": [\"s3:GetBucketLocation\", \"s3:ListBucket\"],\n" +
                    "      \"Resource\": [\"arn:aws:s3:::" + bucket + "\"]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
                    "      \"Action\": [\"s3:GetObject\"],\n" +
                    "      \"Resource\": [\"arn:aws:s3:::" + bucket + "/*\"]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(policy)
                    .build());
            log.info("Successfully set public read policy for bucket: {}", bucket);
        } catch (Exception e) {
            log.error("Failed to initialize Minio client or bucket", e);
        }
    }

    public List<MediaDO> getAuthorMedia(Long authorId) {
        return mediaMapper.selectList(new LambdaQueryWrapper<MediaDO>().eq(MediaDO::getAuthorId, authorId));
    }

    public MediaDO getMediaById(Long mediaId) {
        return mediaMapper.selectById(mediaId);
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

    public void processHls(Long mediaId) {
        MediaDO mediaDO = mediaMapper.selectById(mediaId);
        if (mediaDO == null || !"video".equals(mediaDO.getFileType())) return;

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("video_proc_" + mediaId);
            String inputFileName = mediaDO.getFileKey();
            Path inputPath = tempDir.resolve(inputFileName);

            // 1. 从 MinIO 下载原始视频
            log.info("Downloading original video for mediaId: {}", mediaId);
            minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                            .bucket(bucket)
                            .object(inputFileName)
                            .filename(inputPath.toString())
                            .build()
            );

            // 2. 使用 FFmpeg 进行 HLS 切片
            log.info("Starting FFmpeg HLS slicing for mediaId: {}", mediaId);
            String outputFileName = mediaId + ".m3u8";
            Path outputPath = tempDir.resolve(outputFileName);
            
            // 改进 FFmpeg 命令：增加编码参数以提高兼容性，并减少日志干扰
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", inputPath.toString(),
                "-c:v", "libx264", "-c:a", "aac", // 显式指定编码器
                "-strict", "-2",
                "-profile:v", "baseline", "-level", "3.0",
                "-s", "1280x720", "-start_number", "0",
                "-hls_time", "10", "-hls_list_size", "0",
                "-f", "hls", outputPath.toString()
            );
            pb.redirectErrorStream(true); // 将标准错误合并到标准输出
            Process process = pb.start();
            
            // 实时读取 FFmpeg 输出，防止缓冲区溢出导致死锁
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg output [mediaId={}]: {}", mediaId, line);
                }
            }

            boolean finished = process.waitFor(15, TimeUnit.MINUTES);
            int exitCode = process.exitValue();
            log.info("FFmpeg process finished for mediaId: {}. Exit code: {}, Timeout: {}", mediaId, exitCode, !finished);
            
            if (!finished || exitCode != 0) {
                throw new RuntimeException("FFmpeg processing failed with exit code " + exitCode);
            }

            // 2.5 提取第一帧作为封面图
            log.info("Extracting cover image for mediaId: {}", mediaId);
            String coverFileName = "cover.jpg";
            Path coverPath = tempDir.resolve(coverFileName);
            ProcessBuilder coverPb = new ProcessBuilder(
                "ffmpeg", "-i", inputPath.toString(),
                "-ss", "00:00:01", "-vframes", "1",
                "-q:v", "2", coverPath.toString()
            );
            coverPb.start().waitFor(30, TimeUnit.SECONDS);

            // 3. 上传切片后的文件 (.m3u8 和 .ts) 以及封面图
            log.info("Uploading HLS slices and cover for mediaId: {}", mediaId);
            String finalCoverUrl = "";
            
            // 上传封面
            if (Files.exists(coverPath)) {
                String coverObjectPath = "hls/" + mediaId + "/" + coverFileName;
                minioClient.uploadObject(
                    UploadObjectArgs.builder()
                        .bucket(bucket)
                        .object(coverObjectPath)
                        .filename(coverPath.toString())
                        .contentType("image/jpeg")
                        .build()
                );
                finalCoverUrl = (publicEndpoint != null && !publicEndpoint.isBlank() ? publicEndpoint : endpoint) 
                        + "/" + bucket + "/" + coverObjectPath;
            }

            Files.list(tempDir).forEach(path -> {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".m3u8") || fileName.endsWith(".ts")) {
                    try {
                        String objectPath = "hls/" + mediaId + "/" + fileName;
                        log.debug("Uploading HLS fragment: {} to {}", fileName, objectPath);
                        minioClient.uploadObject(
                            UploadObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectPath)
                                .filename(path.toString())
                                .contentType(fileName.endsWith(".m3u8") ? "application/x-mpegURL" : "video/MP2T")
                                .build()
                        );
                    } catch (Exception e) {
                        log.error("Failed to upload HLS file: {}", fileName, e);
                    }
                }
            });

            // 4. 更新 MediaDO URL 为 .m3u8 地址
            String hlsUrl = (publicEndpoint != null && !publicEndpoint.isBlank() ? publicEndpoint : endpoint) 
                    + "/" + bucket + "/hls/" + mediaId + "/" + outputFileName;
            mediaDO.setUrl(hlsUrl);
            mediaDO.setStatus(MediaDO.STATUS_READY);
            mediaMapper.updateById(mediaDO);
            log.info("Successfully completed HLS processing for mediaId: {}. URL: {}", mediaId, hlsUrl);

            // 5. 发送 Kafka 消息通知核心服务更新视频地址和封面地址
            Map<String, String> payloadMap = Map.of(
                "videoUrl", hlsUrl,
                "videoCoverUrl", finalCoverUrl
            );
            String payload = objectMapper.writeValueAsString(payloadMap);
            kafkaTemplate.send("video-ready-topic", mediaId.toString(), payload);
            log.info("Sent video-ready notification for mediaId: {}. Payload: {}", mediaId, payload);

            Map<String, Object> processedPayload = Map.of(
                "mediaId", mediaId,
                "videoUrl", hlsUrl,
                "videoCoverUrl", finalCoverUrl
            );
            kafkaTemplate.send("media.processed", mediaId.toString(), objectMapper.writeValueAsString(processedPayload));
            log.info("Sent media.processed event for mediaId: {}", mediaId);

        } catch (Exception e) {
            log.error("HLS processing failed for mediaId: {}", mediaId, e);
            updateStatus(mediaId, MediaDO.STATUS_FAILED);
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                        .sorted((p1, p2) -> p2.compareTo(p1))
                        .map(Path::toFile)
                        .forEach(File::delete);
                } catch (Exception ignored) {}
            }
        }
    }

    public Integer getStatus(Long mediaId) {
        MediaDO mediaDO = mediaMapper.selectById(mediaId);
        return mediaDO != null ? mediaDO.getStatus() : null;
    }

    public MediaDO completeUpload(MediaDO mediaDO) {
        if (mediaDO.getUrl() == null || mediaDO.getUrl().isBlank()) {
            mediaDO.setUrl(buildObjectUrl(mediaDO.getFileKey()));
        }
        return registerMedia(mediaDO);
    }

    public void updateStatus(Long mediaId, Integer status) {
        MediaDO mediaDO = mediaMapper.selectById(mediaId);
        if (mediaDO != null) {
            mediaDO.setStatus(status);
            mediaMapper.updateById(mediaDO);
            log.info("Successfully updated media status for {}: {}", mediaId, status);
        }
    }

    public MediaDO registerMedia(MediaDO mediaDO) {
        mediaDO.setStatus(MediaDO.STATUS_PROCESSING);
        mediaDO.setCreateTime(LocalDateTime.now());
        mediaMapper.insert(mediaDO);
        log.info("Successfully registered media: {}", mediaDO.getId());

        sendMediaUploadedEvent(mediaDO);

        if ("video".equals(mediaDO.getFileType())) {
            processVideo(mediaDO.getId());
        } else {
            updateStatus(mediaDO.getId(), MediaDO.STATUS_READY);
            sendMediaProcessedEvent(mediaDO);
        }
        return mediaDO;
    }

    public void saveDanmaku(DanmakuDO danmakuDO) {
        danmakuDO.setCreateTime(LocalDateTime.now());
        danmakuMapper.insert(danmakuDO);
        log.info("Successfully saved danmaku to DB for video {}: {}", danmakuDO.getVideoId(), danmakuDO.getContent());
    }

    public List<DanmakuDO> getDanmakuRange(Long videoId, Long fromMs, Long toMs) {
        return danmakuMapper.selectList(new LambdaQueryWrapper<DanmakuDO>()
                .eq(DanmakuDO::getVideoId, videoId)
                .ge(DanmakuDO::getTimeOffsetMs, fromMs)
                .le(DanmakuDO::getTimeOffsetMs, toMs));
    }

    public DanmakuDO deleteDanmaku(Long danmakuId, Long userId) {
        DanmakuDO danmakuDO = danmakuMapper.selectById(danmakuId);
        if (danmakuDO == null) {
            return null;
        }
        if (!userId.equals(danmakuDO.getUserId())) {
            return null;
        }
        danmakuMapper.deleteById(danmakuId);
        return danmakuDO;
    }

    public void deleteMedia(MediaDO mediaDO) {
        if (mediaDO == null) {
            return;
        }
        mediaMapper.deleteById(mediaDO.getId());
        deleteMediaObjects(mediaDO);
    }

    private void sendMediaUploadedEvent(MediaDO mediaDO) {
        try {
            Map<String, Object> payload = Map.of(
                "mediaId", mediaDO.getId(),
                "fileKey", mediaDO.getFileKey(),
                "fileType", mediaDO.getFileType(),
                "url", mediaDO.getUrl()
            );
            kafkaTemplate.send("media.uploaded", mediaDO.getId().toString(), objectMapper.writeValueAsString(payload));
            log.info("Sent media.uploaded event for mediaId: {}", mediaDO.getId());
        } catch (Exception e) {
            log.warn("Failed to send media.uploaded event for mediaId: {}", mediaDO.getId(), e);
        }
    }

    private void sendMediaProcessedEvent(MediaDO mediaDO) {
        try {
            Map<String, Object> payload = Map.of(
                "mediaId", mediaDO.getId(),
                "url", mediaDO.getUrl()
            );
            kafkaTemplate.send("media.processed", mediaDO.getId().toString(), objectMapper.writeValueAsString(payload));
            log.info("Sent media.processed event for mediaId: {}", mediaDO.getId());
        } catch (Exception e) {
            log.warn("Failed to send media.processed event for mediaId: {}", mediaDO.getId(), e);
        }
    }

    private String buildObjectUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String base = (publicEndpoint != null && !publicEndpoint.isBlank()) ? publicEndpoint : endpoint;
        return base + "/" + bucket + "/" + objectKey;
    }

    private void deleteMediaObjects(MediaDO mediaDO) {
        try {
            if (mediaDO.getFileKey() != null && !mediaDO.getFileKey().isBlank()) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(mediaDO.getFileKey())
                        .build());
            }
            if ("video".equals(mediaDO.getFileType())) {
                String prefix = "hls/" + mediaDO.getId() + "/";
                Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .recursive(true)
                        .build());
                for (Result<Item> itemResult : items) {
                    Item item = itemResult.get();
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete media objects for mediaId: {}", mediaDO.getId(), e);
        }
    }
}

