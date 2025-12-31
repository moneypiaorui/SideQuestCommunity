package com.sidequest.core.interfaces.dto;

import lombok.Data;

@Data
public class CollectionRequest {
    private String name;
    private String description;
    private String coverUrl;
}
