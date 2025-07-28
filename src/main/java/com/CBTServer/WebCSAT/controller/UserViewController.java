package com.CBTServer.WebCSAT.controller;


import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.dto.SubclassCsatDateDTO;
import com.CBTServer.WebCSAT.dto.UserDTO;
import com.CBTServer.WebCSAT.service.SubclassCsatDateService;
import com.CBTServer.WebCSAT.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class UserViewController {
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final SubclassCsatDateService subclassCsatDateService;

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


    @GetMapping("/myPage")
    public String myPage() {
        return "myInfo";
    }

    @GetMapping("/admin/userRole")
    public String userRole() {
        return "userRoleManage";
    }

    @GetMapping("/admin/subjectManage")
    public String subjectManage() {
        return "subjectManage";
    }

    @GetMapping("/admin/examInfo")
    public String examInfo(@RequestParam(required = false) String subject,
                           @RequestParam(required = false) String csatDate,
                           Model model) {
        List<SubclassCsatDateDTO> examList = subclassCsatDateService.getFilteredSubclassCsatDates(subject, csatDate);
        model.addAttribute("examList", examList);
        return "examInfo";
    }
    @GetMapping("/admin/listeningExamInfo")
    public String listeningExamInfo(Model model) {
        List<SubclassCsatDateDTO> examList = subclassCsatDateService.getListeningSubclassCsatDates();
        model.addAttribute("listeningList", examList);
        return "listeningExamInfo";
    }

}
