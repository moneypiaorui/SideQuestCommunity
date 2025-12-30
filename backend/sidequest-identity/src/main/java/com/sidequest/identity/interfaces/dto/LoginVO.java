package com.sidequest.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long expireIn; // 秒
    private Long userId;
    private String nickname;
    private String avatar;
}




