package com.sidequest.search.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sidequest.search.domain.PostRepository;
import com.sidequest.search.infrastructure.PostDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {
    
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "post-topic", groupId = "search-group")
    public void onPostEvent(String message) {
        try {
            log.info("Received post event: {}", message);
            Map<String, Object> postMap = objectMapper.readValue(message, Map.class);
            
            PostDoc doc = new PostDoc();
            doc.setId(postMap.get("id").toString());
            doc.setTitle((String) postMap.get("title"));
            doc.setContent((String) postMap.get("content"));
            doc.setAuthorName((String) postMap.getOrDefault("authorName", "Unknown"));
            doc.setSectionId(postMap.get("sectionId") != null ? Long.valueOf(postMap.get("sectionId").toString()) : null);
            doc.setStatus((Integer) postMap.get("status"));
            doc.setLikeCount((Integer) postMap.getOrDefault("likeCount", 0));
            doc.setCommentCount((Integer) postMap.getOrDefault("commentCount", 0));
            doc.setFavoriteCount((Integer) postMap.getOrDefault("favoriteCount", 0));
            doc.setViewCount((Integer) postMap.getOrDefault("viewCount", 0));
            doc.setCreateTime(System.currentTimeMillis());
            
            postRepository.save(doc);
            log.info("Successfully indexed post: {}", doc.getId());
        } catch (Exception e) {
            log.error("Error processing post event for search index", e);
        }
    }

    @KafkaListener(topics = "post-delete-topic", groupId = "search-group")
    public void onPostDelete(String postId) {
        try {
            log.info("Received post delete event for id: {}", postId);
            postRepository.deleteById(postId);
            log.info("Successfully deleted post index: {}", postId);
        } catch (Exception e) {
            log.error("Error processing post delete event", e);
        }
    }
}

