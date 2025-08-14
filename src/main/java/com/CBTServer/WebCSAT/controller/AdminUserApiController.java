// src/main/java/com/CBTServer/WebCSAT/controller/AdminUserApiController.java
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.dto.UserDTO;
import com.CBTServer.WebCSAT.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApiController {
    private final UserService userService;

    @GetMapping("/users")
    public ArrayList<UserDTO> listUsers(){
        return userService.apiAllUserInfo();
    }
}
