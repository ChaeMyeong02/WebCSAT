package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.dto.ExamDTO;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubmittedAnswerDTO;
import com.CBTServer.WebCSAT.repository.*;
import com.CBTServer.WebCSAT.service.ExamService;
import com.CBTServer.WebCSAT.service.QuestionService;
import com.CBTServer.WebCSAT.service.SubjectSubclassService;
import com.CBTServer.WebCSAT.service.UserDetailService;
import com.amazonaws.services.ec2.model.UserData;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import java.util.Comparator;
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

        // ✅ 선택과목만 전달 (공통과목 제외)
        model.addAttribute("subclasses", subjectSubclassService.findSubclassByOptional(true));

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
            if ("문제은행".equals(questionType)) {
                csatDate = "문제은행용";
            }

            if (!"문제은행".equals(questionType) && (csatDate == null || csatDate.isEmpty())) {
                model.addAttribute("error", "응시 월이 필요합니다.");
                return "error";
            }

            // ✅ 선택 subclass 문제
            List<Question> questions = questionService.getQuestions(subclassId, csatDate);

            // ✅ 공통 subclass 문제도 추가
            Subclass selectedSubclass = subclassRepository.findById(subclassId).orElse(null);
            if (selectedSubclass != null && selectedSubclass.getSubject() != null) {
                Long subjectId = selectedSubclass.getSubject().getSubjectId();
                List<Subclass> commons = subclassRepository.findBySubjectSubjectIdAndOptional(subjectId, false);
                for (Subclass common : commons) {
                    questions.addAll(questionService.getQuestions(common.getSubclassId(), csatDate));
                    questions.sort(Comparator
                            .comparing((Question q) -> q.getSubclass().getSubclassId())
                            .thenComparingInt(Question::getNum));
                }
            }

            // ✅ 더미 문제 대체
            if (questions == null || questions.isEmpty()) {
                String dummyImageUrl = "https://imandjangcbt-bucket.s3.ap-southeast-2.amazonaws.com/문제은행용/더미.png";
                ImageMeta dummyImage = imageMetaRepository.findById(dummyImageUrl).orElse(null);

                Question sample = Question.builder()
                        .questionId(0L)
                        .questionTitle("더미 문제입니다.")
                        .questionArticle(dummyImage)
                        .questionContext(null)
                        .option1("보기 1")
                        .option2("보기 2")
                        .option3("보기 3")
                        .option4("보기 4")
                        .option5("보기 5")
                        .score(0)
                        .questionType(true)
                        .csatDate(null)
                        .build();

                questions = List.of(sample);
                model.addAttribute("isDummy", true);
            }

            // ✅ 과목 및 부과목 정보
            String subclassName = selectedSubclass != null ? selectedSubclass.getSubclassName() : "null";
            String subjectName = (selectedSubclass != null && selectedSubclass.getSubject() != null)
                    ? selectedSubclass.getSubject().getSubjectName() : "null";
            int examTimeMinutes = (selectedSubclass != null && selectedSubclass.getSubject() != null)
                    ? selectedSubclass.getSubject().getMin() : 50;

            model.addAttribute("subjectName", subjectName);
            model.addAttribute("subclassName", subclassName);
            model.addAttribute("examTimeMinutes", examTimeMinutes);
            model.addAttribute("subclassId", subclassId);
            model.addAttribute("csatDate", csatDate);

            // ✅ DTO 변환
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            List<QuestionDTO> dtoList = questions.stream()
                    .map(QuestionDTO::new)
                    .toList();

            model.addAttribute("questionsJson", mapper.writeValueAsString(dtoList));

            // ✅ 공통 지문 범위 계산
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
            model.addAttribute("articleRangeMapJson", mapper.writeValueAsString(articleRangeMap));

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
                         @ModelAttribute ExamDTO dto,
                             HttpSession session) {

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
        // 세션에 examId 저장
        session.setAttribute("lastExamId", savedExam.getExamId());

        // examResult로 리다이렉트(파라미터 없이)
        return "redirect:/examResult";
    }

    @GetMapping("/examResult")
    public String examResult(HttpSession session, Model model) {
        Long examId = (Long) session.getAttribute("lastExamId");
        if (examId == null) {
            // 세션에 examId가 없으면 적절히 처리 (예: 에러 페이지나 로그인 페이지로 이동)
            return "redirect:/login";
        }

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


