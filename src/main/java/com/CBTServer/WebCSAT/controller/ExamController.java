package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.dto.ExamDTO;
import com.CBTServer.WebCSAT.dto.SubmittedAnswerDTO;
import com.CBTServer.WebCSAT.repository.*;
import com.CBTServer.WebCSAT.service.ExamService;
import com.CBTServer.WebCSAT.service.QuestionService;
import com.CBTServer.WebCSAT.service.SubjectSubclassService;
import com.CBTServer.WebCSAT.service.UserDetailService;
import com.amazonaws.services.ec2.model.UserData;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // ✅ springframework
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Time;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ExamController {

    @Autowired
    private SubjectSubclassService subjectSubclassService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubclassRepository subclassRepository;

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ImageMetaRepository imageMetaRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserDetailService userDetailService;

    @Autowired
    private SubjectRepository subjectRepository;

    @GetMapping("/selectType")
    public String selectTypePage() {
        return "test/selectType"; // 문제은행 / 모의고사 선택 화면
    }

    @PostMapping("/selectSubject")
    public String selectSubjectByType(@RequestParam String questionType, Model model) {
        model.addAttribute("questionType", questionType); // "모의고사" or "문제은행"

        model.addAttribute("subjects", subjectSubclassService.findAllSubject());
        model.addAttribute("subclasses", subjectSubclassService.findAllSubclass());

        // ✅ questionType에 따라 csatDate를 필터링
        List<String> csatDates = questionService.getCsatDatesByType(questionType);
        model.addAttribute("csatDates", csatDates);

        return "test/selectSubject";
    }

    @PostMapping("/takeExam")
    public String takeExam(@RequestParam Long subclassId,
                           @RequestParam String csatDate,
                           @RequestParam(required = false) String questionType,
                           Model model) {
        try {

            // ✅ 1. 문제은행일 경우 csatDate를 강제로 "문제은행용"으로 설정
            if ("문제은행".equals(questionType)) {
                csatDate = "문제은행용";
            }

            // ✅ 2. 모의고사인데 응시월을 선택하지 않은 경우 에러 처리
            if (!"문제은행".equals(questionType) && (csatDate == null || csatDate.isEmpty())) {
                model.addAttribute("error", "응시 월이 필요합니다.");
                return "error";
            }

            List<Question> questions = questionService.getQuestions(subclassId, csatDate);



            // ✅ 부과목 가져오기
            Subclass subclass = subclassRepository.findById(subclassId).orElse(null);
            String subclassName = subclass != null ? subclass.getSubclassName() : "null";

            // ✅ 과목 가져오기
            String subjectName = (subclass != null && subclass.getSubject() != null)
                    ? subclass.getSubject().getSubjectName() : "null";

            if (questions == null || questions.isEmpty()) {
                // ✅ 이미지 URL로 ImageMeta 조회
                String dummyImageUrl = "https://imandjangcbt-bucket.s3.ap-southeast-2.amazonaws.com/%EB%AC%B8%EC%A0%9C%EC%9D%80%ED%96%89%EC%9A%A9/1/ebb722f5-a10e-4bf3-9a23-a8cb51fecfde_1.png";
                ImageMeta articleImage = imageMetaRepository.findById(dummyImageUrl).orElse(null);

                Question sample = Question.builder()
                        .questionId(0L)
                        .questionTitle("더미 문제 제목입니다.")
                        .questionArticle(articleImage)
                        .questionContext(null)
                        .option1("1번 보기")
                        .option2("2번 보기")
                        .option3("3번 보기")
                        .option4("4번 보기")
                        .option5("5번 보기")
                        .score(0)
                        .questionType(true)
                        .csatDate(null)
                        .build();

                model.addAttribute("questions", List.of(sample));
                model.addAttribute("subclassId", 0);
                model.addAttribute("csatDate", "더미용");
                model.addAttribute("isDummy", true);
                return "test/takeExam";
            }

            model.addAttribute("questions", questions);
            model.addAttribute("subclassId", subclassId);
            model.addAttribute("csatDate", csatDate);

            // ✅ 제목 관련 이름도 추가
            model.addAttribute("subjectName", subjectName);
            model.addAttribute("subclassName", subclassName);

            // 공통 지문 범위 계산 (url -> [startIndex, endIndex])
            Map<String, int[]> articleRangeMap = new HashMap<>();
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                if (q.getQuestionArticle() == null || q.getQuestionArticle().getUrl() == null) continue;

                String url = q.getQuestionArticle().getUrl();
                if (!articleRangeMap.containsKey(url)) {
                    articleRangeMap.put(url, new int[]{i, i});
                } else {
                    articleRangeMap.get(url)[1] = i;
                }
            }
            model.addAttribute("articleRangeMap", articleRangeMap);


            int total = questions.size();
            int mid = (int) Math.ceil(total / 2.0); // 홀수면 왼쪽이 더 많음

            List<Question> leftQuestions = questions.subList(0, mid);
            List<Question> rightQuestions = questions.subList(mid, total);

            model.addAttribute("leftQuestions", leftQuestions);
            model.addAttribute("rightQuestions", rightQuestions);

            return "test/takeExam";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "시험 데이터를 불러오는 중 오류 발생: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/takeBankExam")
    public String takeBankExam(@RequestParam Long subjectId,
                               @RequestParam Long subclassId, Model model) {
        String csatDate = "문제은행용";
        List<Question> questions = questionService.getQuestions(subclassId, csatDate);

        if (questions == null || questions.isEmpty()) {
            model.addAttribute("error", "문제은행에 해당 과목의 문제가 없습니다.");
            return "error";
        }

        // ✅ 문제 수가 1개 이하면 종료 알림 플래그 전달
        if (questions.size() <= 1) {
            model.addAttribute("finished", true); // ✅ 이 플래그를 bankAnswerResult에서 사용할 예정
            return "test/bankAnswerResult";
        }

        model.addAttribute("question", questions.get(0));  // 첫 문제만 표시
        model.addAttribute("currentIndex", 0);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("questions", questions);  // 인덱스로 다음 문제 찾을 수 있도록
        model.addAttribute("subjectId", subjectId);  // ✅ 여기도 추가!
        model.addAttribute("subclassId", subclassId); // ✅ 이것도 확실히 넣기!
        return "test/takeBankExam";  // 한 문제씩 보여주는 뷰
    }

    @PostMapping("/submitBankAnswer")
    public String submitBankAnswer(@RequestParam Long questionId,
                                   @RequestParam String answer,
                                   @RequestParam Long subclassId,
                                   @RequestParam Long subjectId,
                                   Model model) {

        Question question = questionService.findById(questionId);
        boolean isCorrect = question.getAnswer().equals(answer);

        model.addAttribute("isCorrect", isCorrect);
        model.addAttribute("question", question);
        model.addAttribute("subclassId", subclassId);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("finished", false);
        return "test/bankAnswerResult";
    }


    @PostMapping("/submitExam")
    public String submitExam(HttpServletRequest request,
                         @RequestParam Long subclassId,
                         @RequestParam String csatDate,
                         @RequestParam int elapsedTime,
                         @ModelAttribute ExamDTO dto) {

        // JWT 토큰 → 유저 이메일 꺼내기
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
        return "redirect:/login";
        }

        token = token.substring(7); // "Bearer " 제거
        String email = tokenProvider.getClaims(token).getSubject();
        User user = userDetailService.loadUserByUsername(email);

        // 시험 저장
        Subclass subclass = subclassRepository.findById(subclassId)
                .orElseThrow(() -> new RuntimeException("부과목 없음: " + subclassId));

        List<SubmittedAnswerDTO> answerList = dto.toSubmittedAnswerList();
        Exam savedExam = examService.submitExam(user, subclass, csatDate, answerList, elapsedTime);

        // ✅ examId를 포함해 결과 페이지로 리디렉션
        return "redirect:/examResult?examId=" + savedExam.getExamId();
    }

    @GetMapping("/examResult")
    public String examResult(@RequestParam Long examId, Model model) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("시험 없음: " + examId));

        model.addAttribute("exam", exam);

        // 시간 포맷 추가 (옵션)
        Time duration = exam.getDuration();
        String formattedDuration = (duration != null)
                ? duration.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                : "-";
        model.addAttribute("formattedDuration", formattedDuration);

        return "test/examResult";
    }
}


