package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/* User Data Transfer Object */
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class UserDTO {
    private Long userId;
    private String email;
    private String password;
    private String nickname;
    private String oauth2;
    private String role;
    private LocalDateTime created_at;
}
