package com.sidequest.core.interfaces.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String content;
    private Long sectionId;
    private Integer status;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    private List<String> imageUrls; 
    private String videoUrl;
    private String videoCoverUrl;
    private Integer videoDuration;
    private List<String> tags;

    private boolean hasLiked;
    private boolean hasFavorited;
}

