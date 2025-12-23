package com.sidequest.core.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Post {
    private Long id;
    private Long authorId;
    private String title;
    private String content;
    private List<String> imageUrls;
    private String videoUrl;
    private Long sectionId;
    private List<String> tags;
    private LocalDateTime createTime;
    private Integer status; // 0: Published, 1: Draft, 2: Deleted

    // 充血模型：包含业务逻辑
    public void publish() {
        if (this.title == null || this.title.trim().isEmpty()) {
            throw new RuntimeException("Post title cannot be empty");
        }
        this.status = 0;
        this.createTime = LocalDateTime.now();
    }

    public void archive() {
        this.status = 2;
    }
    
    public boolean canBeEditedBy(Long userId) {
        return this.authorId.equals(userId);
    }
}
