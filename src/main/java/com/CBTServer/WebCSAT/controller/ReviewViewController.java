// src/main/java/.../controller/ReviewViewController.java
package com.CBTServer.WebCSAT.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReviewViewController {
    @GetMapping("/review")
    public String reviewPage() {
        return "review"; // templates/review.html
    }
}
