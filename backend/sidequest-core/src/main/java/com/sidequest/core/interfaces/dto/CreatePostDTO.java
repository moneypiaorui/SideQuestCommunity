package com.sidequest.core.interfaces.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePostDTO {
    private String title;
    private String content;
    private Long sectionId;
    private List<String> tags;
}

