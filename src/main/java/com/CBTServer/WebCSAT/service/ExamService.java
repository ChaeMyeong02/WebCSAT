package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import com.CBTServer.WebCSAT.repository.ExamRepository;
import com.CBTServer.WebCSAT.repository.ExamQuestionRepository;
import com.CBTServer.WebCSAT.repository.QuestionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.CBTServer.WebCSAT.domain.Subclass;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.CBTServer.WebCSAT.dto.SubmittedAnswer;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private CsatDateRepository csatDateRepository;


    @Transactional
    public Exam submitExam(User user, Subclass subclass, String csatDate, List<SubmittedAnswer> submittedAnswers)
    {
        // 1. 시험 객체 생성
        Exam exam = new Exam();
        exam.setUser(user);
        exam.setSubclass(subclass);
        CsatDate csatDateEntity = csatDateRepository.findById(csatDate)
                .orElseThrow(() -> new RuntimeException("해당 시험 날짜 없음: " + csatDate));
        exam.setCsatDate(csatDateEntity);
        exam.setTestDate(LocalDateTime.now());

        // 임시: 시작 시간 ~ 현재 시간 (예: 30분)
        Duration duration = Duration.ofMinutes(30);
        exam.setDuration(new Time(duration.toMillis()));

        // 2. ExamQuestion 생성
        List<ExamQuestion> examQuestions = new ArrayList<>();
        int totalScore = 0;

        for (SubmittedAnswer submitted : submittedAnswers) {
            Question question = questionRepository.findById(submitted.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("문제 없음: " + submitted.getQuestionId()));

            boolean isCorrect = question.getAnswer().equals(submitted.getSelectedAnswer());

            ExamQuestion eq = new ExamQuestion();
            eq.setExam(exam);
            eq.setSubclass(subclass);
            eq.setQuestion(question);
            eq.setSelectedAnswer(submitted.getSelectedAnswer());
            eq.setIsCorrect(isCorrect);

            examQuestions.add(eq);

            if (isCorrect) {
                totalScore += question.getScore(); // 정답일 경우 점수 추가
            }
        }

        exam.setResult(totalScore);

        // 3. 저장
        examRepository.save(exam);
        examQuestionRepository.saveAll(examQuestions);

        return exam;
    }
}
