package com.CBTServer.WebCSAT.dto;

import com.CBTServer.WebCSAT.domain.User;
import lombok.*;

import java.time.LocalDateTime;

/* User Data Transfer Object */
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class UserDTO {
    private Long userId;
    private String email;
    private String password;
    private String nickname;
    private String oauth2;
    private String role;
    private LocalDateTime created_at;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .oauth2(user.getOauth2())
                .role(user.getRole())
                .created_at(user.getCreatedAt())
                .build();
    }
}
