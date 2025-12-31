package com.sidequest.chat.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateRoomRequest {
    private String name;
    private String type; // GROUP or PRIVATE
    private List<Long> memberIds;
}
