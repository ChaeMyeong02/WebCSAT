package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import com.CBTServer.WebCSAT.repository.ExamRepository;
import com.CBTServer.WebCSAT.repository.ExamQuestionRepository;
import com.CBTServer.WebCSAT.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.CBTServer.WebCSAT.dto.SubmittedAnswerDTO;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final CsatDateRepository csatDateRepository;

    @Transactional
    public Exam submitExam(User user,
                           Subclass subclass,
                           String csatDate,
                           List<SubmittedAnswerDTO> submittedAnswers,
                           int elapsedTime) {

        // 1) 시험 엔터티 기본정보
        Exam exam = new Exam();
        exam.setUser(user);
        exam.setSubclass(subclass);
        CsatDate csatDateEntity = csatDateRepository.findById(csatDate)
                .orElseThrow(() -> new RuntimeException("해당 시험 날짜 없음: " + csatDate));
        exam.setCsatDate(csatDateEntity);
        exam.setTestDate(LocalDateTime.now());

        // 경과시간 저장 (초 → LocalTime)
        Duration duration = Duration.ofSeconds(elapsedTime);
        LocalTime localTime = LocalTime.ofSecondOfDay(duration.getSeconds());
        exam.setDuration(Time.valueOf(localTime));

        // 2) 문항 채점
        List<ExamQuestion> examQuestions = new ArrayList<>();
        int totalScore = 0;

        for (SubmittedAnswerDTO submitted : submittedAnswers) {
            // 문제 조회
            Question question = questionRepository.findById(submitted.getQuestionId())
                    .orElseThrow(() ->
                            new RuntimeException("문제 없음: " + submitted.getQuestionId()));

            // --- null-safe 정규화 ---
            String correct = question.getAnswer();
            correct = (correct == null ? null : correct.trim());

            String userSel = submitted.getSelectedAnswer();
            if (userSel != null) userSel = userSel.trim();
            // 프런트에서 미응답을 -1/0/"" 로 보낼 수 있으므로 null 처리
            if (userSel == null || userSel.isEmpty() ||
                    "-1".equals(userSel) || "0".equals(userSel)) {
                userSel = null;
            }

            // 정답이 null이면 오답 처리(점수 미가산) + 경고 로그
            if (correct == null) {
                log.warn("Question {} has NULL answer (csatDate={}, num={})",
                        question.getQuestionId(), question.getCsatDate(), question.getNum());
            }

            boolean isCorrect = (correct != null && userSel != null && correct.equals(userSel));

            // ExamQuestion 생성
            ExamQuestion eq = new ExamQuestion();
            eq.setExam(exam);
            eq.setSubclass(subclass);
            eq.setQuestion(question);
            eq.setSelectedAnswer(userSel); // 미응답이면 null 저장
            eq.setCorrect(isCorrect);
            examQuestions.add(eq);

            if (isCorrect) {
                Integer sc = question.getScore();
                totalScore += (sc != null ? sc : 0);
            }
        }

        exam.setResult(totalScore);

        // 3) 저장
        examRepository.save(exam);
        examQuestionRepository.saveAll(examQuestions);

        return exam;
    }
}
