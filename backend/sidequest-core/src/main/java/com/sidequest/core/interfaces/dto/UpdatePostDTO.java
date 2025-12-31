package com.sidequest.core.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePostDTO {
    private String title;
    private String content;
    private Long sectionId;
    private List<String> tags;
    private List<String> imageUrls;
    private String videoUrl;
    private String videoCoverUrl;
    private Integer videoDuration;
    private Long mediaId;
}
