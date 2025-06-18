package com.CBTServer.WebCSAT.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebApiController {

    @GetMapping("/")
    public String index() {
        return "hello world";
    }
}
