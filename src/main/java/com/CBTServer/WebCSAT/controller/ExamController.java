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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // ✅ springframework
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    // 공통 빌더
    private String buildSelectSubject(String questionType, Model model) {
        model.addAttribute("questionType", questionType);

        // subject는 optional 영향 X → 전체
        model.addAttribute("subjects", subjectSubclassService.findAllSubject());
        // subclass는 화면에서 선택과목만 보여줄 거면 optional=true만
        model.addAttribute("subclasses", subjectSubclassService.findSubclassByOptional(true));

        List<String> csatDates = questionService.getCsatDatesByType(questionType);
        model.addAttribute("csatDates", csatDates);
        return "test/selectSubject";
    }

    // GET 버튼 전환용 (모드 토글 클릭)
    @GetMapping("/selectSubject")
    public String selectSubjectByTypeGet(
            @RequestParam(defaultValue = "모의고사") String questionType, Model model) {
        return buildSelectSubject(questionType, model);
    }

    // POST 기존 흐름(이전 페이지에서 넘어올 때)
    @PostMapping("/selectSubject")
    public String selectSubjectByTypePost(
            @RequestParam(defaultValue = "모의고사") String questionType, Model model) {
        return buildSelectSubject(questionType, model);
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

    // ── 한 문제씩 풀기용: 문제 출제 순서를 세션에 보관 ──
    private String orderKey(Long subjectId, Long subclassId) {
        return "bankOrder-" + subjectId + "-" + subclassId;
    }

    @SuppressWarnings("unchecked")
    private List<Long> getOrCreateOrder(Long subjectId, Long subclassId,
                                        HttpSession session, List<Question> qs) {
        String key = orderKey(subjectId, subclassId);
        List<Long> order = (List<Long>) session.getAttribute(key);

        // 없거나 길이/구성이 달라졌으면 새로 셔플
        if (order == null || order.size() != qs.size()) {
            order = new ArrayList<>();
            for (Question q : qs) order.add(q.getQuestionId());
            Collections.shuffle(order);                // ★ 여기서 랜덤화
            session.setAttribute(key, order);
        }
        return order;
    }

    private void resetOrder(Long subjectId, Long subclassId, HttpSession session) {
        session.removeAttribute(orderKey(subjectId, subclassId));
    }


    private List<Question> buildBankQuestions(Long subjectId, Long subclassId) {
        final String csatDate = "문제은행용";

        // 선택 서브클래스 문제
        List<Question> merged = new java.util.ArrayList<>(
                questionService.getQuestions(subclassId, csatDate)
        );

        // 같은 과목의 공통 서브클래스(optional=false) 추가
        Subclass selected = subclassRepository.findById(subclassId).orElse(null);
        Long subjectIdResolved = (selected != null && selected.getSubject() != null)
                ? selected.getSubject().getSubjectId()
                : subjectId; // 폼에서 넘어온 subjectId로 fallback

        List<Subclass> commons = subclassRepository
                .findBySubjectSubjectIdAndOptional(subjectIdResolved, false); // optional=0

        for (Subclass c : commons) {
            merged.addAll(questionService.getQuestions(c.getSubclassId(), csatDate));
        }

        // 정렬: 서브클래스ID → 문항번호
        merged.sort(Comparator
                .comparing((Question q) -> q.getSubclass().getSubclassId())
                .thenComparingInt(Question::getNum));

        // (선택) 중복 제거: 같은 questionId가 중복 로드되는 경우 대비
        java.util.LinkedHashMap<Long, Question> dedup = new java.util.LinkedHashMap<>();
        for (Question q : merged) dedup.put(q.getQuestionId(), q);
        return new java.util.ArrayList<>(dedup.values());
    }

    @PostMapping("/takeBankExam")
    public String takeBankExam(@RequestParam Long subjectId,
                               @RequestParam Long subclassId,
                               @RequestParam(defaultValue = "0") int index,
                               Model model,
                               HttpSession session) {

        List<Question> all = buildBankQuestions(subjectId, subclassId);
        if (all.isEmpty()) {
            model.addAttribute("error", "문제은행에 해당 과목의 문제가 없습니다.");
            return "error";
        }

        // 세션의 랜덤 순서 확보(없으면 생성)
        List<Long> order = getOrCreateOrder(subjectId, subclassId, session, all);

        if (index >= order.size()) {
            model.addAttribute("finished", true);
            model.addAttribute("subjectId", subjectId);
            model.addAttribute("subclassId", subclassId);
            return "test/bankAnswerResult";
        }

        Long qid = order.get(index);
        Question q = questionService.findById(qid);

        model.addAttribute("question", q);
        model.addAttribute("currentIndex", index);
        model.addAttribute("totalQuestions", order.size());
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("subclassId", subclassId);
        return "test/takeBankExam";
    }



    @PostMapping("/submitBankAnswer")
    public String submitBankAnswer(@RequestParam(required=false) String answer,
                                   @RequestParam Long subclassId,
                                   @RequestParam Long subjectId,
                                   @RequestParam int currentIndex,
                                   Model model,
                                   HttpSession session) {

        List<Question> all = buildBankQuestions(subjectId, subclassId);
        List<Long> order = getOrCreateOrder(subjectId, subclassId, session, all);

        Long qid = order.get(currentIndex);
        Question question = questionService.findById(qid);

        // 정답 비교 (DB에 기호가 섞여 있을 수 있으니 숫자화)
        String sel = normalizeAnswer(answer);
        String cor = normalizeAnswer(question.getAnswer());
        boolean isCorrect = (sel != null) && sel.equals(cor);

        int nextIndex = currentIndex + 1;
        boolean finished = (nextIndex >= order.size());

        model.addAttribute("isCorrect", isCorrect);
        model.addAttribute("question", question);
        model.addAttribute("nextIndex", nextIndex);
        model.addAttribute("finished", finished);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("subclassId", subclassId);
        return "test/bankAnswerResult";
    }

    @GetMapping("/takeBankSet")
    public String takeBankSetGet(@RequestParam Long subjectId,
                                 @RequestParam Long subclassId,
                                 Model model) throws JsonProcessingException {
        return buildTakeBankSetPage(subjectId, subclassId, model);
    }

    @PostMapping("/takeBankSet")
    public String takeBankSetPost(@RequestParam Long subjectId,
                                  @RequestParam Long subclassId,
                                  Model model) throws JsonProcessingException {
        return buildTakeBankSetPage(subjectId, subclassId, model);
    }

    // 공용 로직
    private String buildTakeBankSetPage(Long subjectId, Long subclassId, Model model)
            throws JsonProcessingException {

        List<QuestionDTO> dtoList = buildBankQuestions(subjectId, subclassId)
                .stream().map(QuestionDTO::new).toList();

        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        model.addAttribute("questionsJson", om.writeValueAsString(dtoList)); // JSON 문자열
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("subclassId", subclassId);
        return "test/takeBankSet";
    }

    // ExamController 내부

    public static class BankRow {
        private final Question q;
        private final String selected;  // 사용자가 고른 값: "1"~"5" 또는 ""
        private final String correct;   // 정규화된 정답: "1"~"5"
        public BankRow(Question q, String selected, String correct){
            this.q = q; this.selected = selected; this.correct = correct;
        }
        public Question getQ() { return q; }
        public String getSelected() { return selected; }
        public String getCorrect() { return correct; }
    }

    // "①②③④⑤", "3 ", "3번" 등 → "1"~"5" 로 통일
    private static String normalizeAnswer(String a){
        if (a == null) return "";
        String s = a.trim()
                .replace("①","1").replace("②","2").replace("③","3")
                .replace("④","4").replace("⑤","5");
        // 문자열 안에 1~5가 섞여 있으면 첫 숫자만 사용
        for (int i=0;i<s.length();i++){
            char c = s.charAt(i);
            if (c>='1' && c<='5') return String.valueOf(c);
        }
        return s; // 혹시 모르니 그대로 반환
    }

    @PostMapping("/submitBankSet")
    public String submitBankSet(@RequestParam List<Long> questionIds,
                                @RequestParam(required=false) List<String> answers,
                                Model model) {
        int total = questionIds.size();
        if (answers == null) answers = Collections.emptyList();

        int correctCnt = 0;
        List<BankRow> rows = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            Question q = questionService.findById(questionIds.get(i));
            String selected = (i < answers.size() && answers.get(i) != null) ? answers.get(i) : "";
            String correct  = normalizeAnswer(q.getAnswer());       // ★ 정답 정규화
            if (selected.equals(correct)) correctCnt++;              // ★ 정규화 기준 비교
            rows.add(new BankRow(q, selected, correct));            // ★ 정규화 값 전달
        }

        model.addAttribute("total",      total);
        model.addAttribute("correctCnt", correctCnt);
        model.addAttribute("wrongCnt",   total - correctCnt);
        model.addAttribute("score",      Math.round(correctCnt * 100.0 / (total == 0 ? 1 : total)));
        model.addAttribute("rows",       rows);
        return "test/bankSetResult";
    }






}