package com.CBTServer.WebCSAT.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // ✅ 이 Model을 사용하세요
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StatsViewController {

    @GetMapping("/stats")
    public String myStatsPage(Model model) {
        // 필요하면 서버에서 미리 넣기
        // model.addAttribute("subclasses", subclassService.findAll());
        // model.addAttribute("userId", currentUserId);
        return "stats"; // templates/stats.html
    }
}
