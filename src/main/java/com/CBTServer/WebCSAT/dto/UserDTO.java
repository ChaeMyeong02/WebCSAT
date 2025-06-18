package com.CBTServer.WebCSAT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* User Data Transfer Object */
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class UserDTO {
    private String email;
    private String password;
    private String nickname;
}
