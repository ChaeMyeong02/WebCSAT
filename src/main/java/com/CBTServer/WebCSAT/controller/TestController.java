package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.repository.QuestionRepository;
import com.CBTServer.WebCSAT.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final QuestionService questionService;
    private final QuestionRepository questionRepository;


    // 시험 응시 화면 (20문제)
    @GetMapping("/take")
    public String showTestPage(Model model, HttpSession session) {
        List<Question> questions = questionRepository.findFirst20ByOrderByQuestionIdAsc();
        model.addAttribute("questions", questions);
        return "test/takeExam";
    }

    // 제출 후 채점 처리
    @PostMapping("/submit")
    public String submitAnswers(@RequestParam Map<String, String> answers, Model model) {
        int score = 0;

        for (String key : answers.keySet()) {
            if (key.startsWith("q")) {
                String submitted = answers.get(key);
                Long qid = Long.parseLong(key.substring(1));
                Question question = questionService.findById(qid);
                if (question.getAnswer().equals(submitted)) {
                    score += question.getScore(); // 정답 시 점수 추가
                }
            }
        }

        model.addAttribute("score", score);
        return "test/examResult";
    }
}
