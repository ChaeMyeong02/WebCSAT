package com.CBTServer.WebCSAT.controller;


import com.CBTServer.WebCSAT.dto.UserDTO;
import com.CBTServer.WebCSAT.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequiredArgsConstructor
@Controller
public class UserViewController {
    private final UserService userService;

    // Default Login GetMapping
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Default SingUp GetMapping
    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

}
