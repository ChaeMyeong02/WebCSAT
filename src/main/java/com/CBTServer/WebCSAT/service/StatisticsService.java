package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.Exam;
import com.CBTServer.WebCSAT.domain.ExamQuestion;
import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.domain.User;
import com.CBTServer.WebCSAT.dto.*;
import com.CBTServer.WebCSAT.repository.ExamQuestionRepository;
import com.CBTServer.WebCSAT.repository.ExamRepository;
import com.CBTServer.WebCSAT.repository.QuestionRepository;
import com.CBTServer.WebCSAT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;

    public List<UserScoreHistoryDTO> getUserScoreHistory() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        List<Exam> exams = examRepository.findByUser(user);

        return exams.stream().map(exam -> {
                    String grade;
                    int score = exam.getResult();
                    if (score >= 90) grade = "1등급";
                    else if (score >= 80) grade = "2등급";
                    else if (score >= 70) grade = "3등급";
                    else if (score >= 60) grade = "4등급";
                    else if (score >= 50) grade = "5등급";
                    else if (score >= 40) grade = "6등급";
                    else if (score >= 30) grade = "7등급";
                    else if (score >= 20) grade = "8등급";
                    else grade = "9등급";

                    return new UserScoreHistoryDTO(
                            exam.getExamId(),
                            exam.getCsatDate().getCsatDate(),
                            exam.getSubclass().getSubclassName(),
                            score,
                            grade
                    );
                }).sorted(Comparator.comparing(UserScoreHistoryDTO::getCsatDate))
                .collect(Collectors.toList());
    }

    public List<GradeTrendDTO> getGradeTrend(String csatDate) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        List<Exam> exams = examRepository.findByUserAndCsatDate_CsatDate(user, csatDate);

        return exams.stream().map(exam -> {
            int score = exam.getResult();
            String grade;
            if (score >= 90) grade = "1등급";
            else if (score >= 80) grade = "2등급";
            else if (score >= 70) grade = "3등급";
            else if (score >= 60) grade = "4등급";
            else if (score >= 50) grade = "5등급";
            else if (score >= 40) grade = "6등급";
            else if (score >= 30) grade = "7등급";
            else if (score >= 20) grade = "8등급";
            else grade = "9등급";

            return new GradeTrendDTO(
                    exam.getSubclass().getSubclassName(),
                    List.of(score),
                    List.of(grade)
            );
        }).collect(Collectors.toList());
    }

    public List<ScoreTrendDTO> getScoreTrend() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        List<Exam> exams = examRepository.findByUser(user);

        return exams.stream()
                .collect(Collectors.groupingBy(e -> e.getSubclass().getSubclassName()))
                .entrySet().stream()
                .map(entry -> {
                    String subclassName = entry.getKey();
                    List<Exam> examList = entry.getValue();
                    List<Integer> scores = examList.stream()
                            .sorted(Comparator.comparing(e -> e.getCsatDate().getCsatDate()))
                            .map(Exam::getResult)
                            .collect(Collectors.toList());
                    return new ScoreTrendDTO(subclassName, scores);
                }).collect(Collectors.toList());
    }

    public List<WrongQuestionDTO> getMostWrongQuestions() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        List<ExamQuestion> wrongQuestions = examQuestionRepository.findByExam_UserAndIsCorrectFalse(user);

        Map<Long, Integer> wrongCountMap = new HashMap<>();
        Map<Long, String> titleMap = new HashMap<>();

        for (ExamQuestion eq : wrongQuestions) {
            Long questionId = eq.getQuestion().getQuestionId();
            wrongCountMap.put(questionId, wrongCountMap.getOrDefault(questionId, 0) + 1);
            titleMap.putIfAbsent(questionId, eq.getQuestion().getQuestionTitle());
        }

        return wrongCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new WrongQuestionDTO(entry.getKey(), titleMap.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    public QuestionStatsDTO getQuestionStats(Long questionId) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByQuestion_QuestionId(questionId);
        int total = examQuestions.size();
        int correct = (int) examQuestions.stream().filter(ExamQuestion::isCorrect).count();

        double correctRate = total > 0 ? (correct * 100.0 / total) : 0.0;

        return new QuestionStatsDTO(questionId, correctRate, total, correct);
    }

    public GradeDistributionDTO getGradeDistribution(String csatDate) {
        List<Exam> exams = examRepository.findByCsatDate_CsatDate(csatDate);

        int[] gradeCounts = new int[9]; // 1~9등급 → 인덱스 0~8

        for (Exam exam : exams) {
            int score = exam.getResult();
            int grade = getGradeIndex(score); // 0~8
            gradeCounts[grade]++;
        }

        return new GradeDistributionDTO(csatDate, Arrays.stream(gradeCounts).boxed().collect(Collectors.toList()));
    }
    private int getGradeIndex(int score) {
        if (score >= 90) return 0;
        if (score >= 80) return 1;
        if (score >= 70) return 2;
        if (score >= 60) return 3;
        if (score >= 50) return 4;
        if (score >= 40) return 5;
        if (score >= 30) return 6;
        if (score >= 20) return 7;
        return 8;
    }

    public PercentileDTO getPercentile(String csatDate) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        // 전체 시험 점수
        List<Exam> allExams = examRepository.findByCsatDate_CsatDate(csatDate);
        List<Integer> allScores = allExams.stream()
                .map(Exam::getResult)
                .sorted(Comparator.reverseOrder()) // 내림차순: 높은 점수가 상위 랭크
                .collect(Collectors.toList());

        // 현재 유저 평균 점수
        List<Exam> myExams = examRepository.findByUserAndCsatDate_CsatDate(user, csatDate);
        double myAvg = myExams.stream()
                .mapToInt(Exam::getResult)
                .average()
                .orElse(0);

        // 랭크 계산 (평균이 전체 몇 등인지)
        int rank = 1;
        for (int score : allScores) {
            if (score <= myAvg) break;
            rank++;
        }

        int total = allScores.size();
        int percentile = (int) ((1 - ((double) rank - 1) / total) * 100); // 상위 퍼센트

        return new PercentileDTO(csatDate, percentile, total, rank);
    }

    public int getExamAttempts() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return examRepository.countByUser(user);
    }

    public QuestionStatsDTO getBankQuestionStats(Long questionId) {
        return getQuestionStats(questionId);
    }

    public List<WrongQuestionDTO> getTopWrongQuestions() {
        List<ExamQuestion> wrongQuestions = examQuestionRepository.findByIsCorrectFalse();

        Map<Long, Integer> wrongCountMap = new HashMap<>();
        Map<Long, String> titleMap = new HashMap<>();

        for (ExamQuestion eq : wrongQuestions) {
            Long questionId = eq.getQuestion().getQuestionId();
            wrongCountMap.put(questionId, wrongCountMap.getOrDefault(questionId, 0) + 1);
            titleMap.putIfAbsent(questionId, eq.getQuestion().getQuestionTitle());
        }

        return wrongCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new WrongQuestionDTO(entry.getKey(), titleMap.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    public UserBaseStatsDTO getUserBaseStats() {
        long totalUsers = userRepository.count();
        long totalExams = examRepository.count();
        long avgExams = totalUsers == 0 ? 0 : totalExams / totalUsers;

        return new UserBaseStatsDTO(totalUsers, totalExams, avgExams);
    }

    public MockParticipationDTO getParticipants(String csatDate) {
        List<Exam> exams = examRepository.findByCsatDate_CsatDate(csatDate);
        long uniqueUsers = exams.stream().map(Exam::getUser).distinct().count();

        return new MockParticipationDTO(csatDate, (int) uniqueUsers);
    }

    public List<SubjectParticipationDTO> getSubjectParticipants() {
        List<Exam> exams = examRepository.findAll();

        return exams.stream()
                .collect(Collectors.groupingBy(e -> e.getSubclass().getSubject().getSubjectName(), Collectors.mapping(Exam::getUser, Collectors.toSet())))
                .entrySet().stream()
                .map(entry -> new SubjectParticipationDTO(entry.getKey(), entry.getValue().size()))
                .collect(Collectors.toList());
    }

    public List<MonthlyParticipantsDTO> getMonthlyParticipants() {
        List<Exam> exams = examRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        return exams.stream()
                .collect(Collectors.groupingBy(e -> e.getTestDate().format(formatter), Collectors.mapping(Exam::getUser, Collectors.toSet())))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MonthlyParticipantsDTO(entry.getKey(), entry.getValue().size()))
                .collect(Collectors.toList());
    }

    private String getGrade(int score) {
        if (score >= 90) return "1등급";
        if (score >= 80) return "2등급";
        if (score >= 70) return "3등급";
        if (score >= 60) return "4등급";
        if (score >= 50) return "5등급";
        if (score >= 40) return "6등급";
        if (score >= 30) return "7등급";
        if (score >= 20) return "8등급";
        return "9등급";
    }
}
