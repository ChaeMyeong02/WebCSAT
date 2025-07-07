package com.CBTServer.WebCSAT.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {
    // Index
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
