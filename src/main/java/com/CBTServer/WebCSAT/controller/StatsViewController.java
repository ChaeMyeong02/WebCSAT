// src/main/java/com/CBTServer/WebCSAT/controller/StatsViewController.java
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class StatsViewController {

    private final CurrentUserService currentUserService;

    @GetMapping("/stats")
    public String stats(@RequestParam(name = "mode", required = false) String mode, Model model) {
        return prepare(model, (mode == null || mode.isBlank()) ? "mine" : mode.toLowerCase());
    }

    // ✅ 추가: /stats/admin → mode=all
    @GetMapping("/stats/admin")
    public String statsAll(Model model) {
        return prepare(model, "all");
    }

    // ✅ 추가: /stats/user → mode=one
    @GetMapping("/stats/user")
    public String statsOne(Model model) {
        return prepare(model, "one");
    }

    private String prepare(Model model, String mode) {
        model.addAttribute("mode", mode);
        Long uid = null;
        try { uid = currentUserService.requiredUserId(); } catch (Exception ignored) {}
        model.addAttribute("userId", uid);
        return "stats"; // templates/stats.html
    }
}
