// controller/ReviewPageController.java
package com.CBTServer.WebCSAT.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ReviewPageController {

    @GetMapping("/review/{examId}")
    public String reviewPage(@PathVariable Long examId, Model model) {
        model.addAttribute("examId", examId);
        return "review"; // templates/review.html
    }

}
