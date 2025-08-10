    package com.CBTServer.WebCSAT.controller;

    import com.CBTServer.WebCSAT.dto.*;
    import com.CBTServer.WebCSAT.service.StatisticsService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/api/stats")
    @RequiredArgsConstructor
    public class StatApiController {

        private final StatisticsService statisticsService;

        // 사용자 통계
        @GetMapping("/user/history")
        public List<UserScoreHistoryDTO> getUserScoreHistory() {
            return statisticsService.getUserScoreHistory();
        }

        @GetMapping("/user/grade-trend")
        public List<GradeTrendDTO> getGradeTrend(@RequestParam String mockName) {
            return statisticsService.getGradeTrend(mockName);
        }

        @GetMapping("/user/score-trend")
        public List<ScoreTrendDTO> getScoreTrend() {
            return statisticsService.getScoreTrend();
        }

        @GetMapping("/user/wrong-questions")
        public List<WrongQuestionDTO> getMostWrongQuestions() {
            return statisticsService.getMostWrongQuestions();
        }

        @GetMapping("/user/question/{id}")
        public QuestionStatsDTO getQuestionStats(@PathVariable Long id) {
            return statisticsService.getQuestionStats(id);
        }

        @GetMapping("/user/grade-distribution")
        public GradeDistributionDTO getGradeDistribution(@RequestParam String csatDate) {
            return statisticsService.getGradeDistribution(csatDate);
        }

        @GetMapping("/user/percentile")
        public PercentileDTO getPercentile(@RequestParam String csatDate) {
            return statisticsService.getPercentile(csatDate);
        }

        @GetMapping("/user/attempts")
        public int getExamAttempts() {
            return statisticsService.getExamAttempts();
        }

        // 문제은행
        @GetMapping("/bank/question/{id}")
        public QuestionStatsDTO getBankQuestionStats(@PathVariable Long id) {
            return statisticsService.getBankQuestionStats(id);
        }

        @GetMapping("/bank/most-wrong")
        public List<WrongQuestionDTO> getTopWrongQuestions() {
            return statisticsService.getTopWrongQuestions();
        }

        // 관리자 통계
        @GetMapping("/admin/user-base")
        public UserBaseStatsDTO getUserBaseStats() {
            return statisticsService.getUserBaseStats();
        }

        @GetMapping("/admin/participants")
        public MockParticipationDTO getParticipants(@RequestParam String csatDate) {
            return statisticsService.getParticipants(csatDate);
        }

        @GetMapping("/admin/subject-participants")
        public List<SubjectParticipationDTO> getSubjectParticipants() {
            return statisticsService.getSubjectParticipants();
        }

        @GetMapping("/admin/monthly-participants")
        public List<MonthlyParticipantsDTO> getMonthlyParticipants() {
            return statisticsService.getMonthlyParticipants();
        }
    }
