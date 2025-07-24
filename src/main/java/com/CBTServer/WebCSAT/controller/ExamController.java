package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.dto.ExamDTO;
import com.CBTServer.WebCSAT.dto.SubmittedAnswerDTO;
import com.CBTServer.WebCSAT.repository.ImageMetaRepository;
import com.CBTServer.WebCSAT.repository.QuestionRepository;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import com.CBTServer.WebCSAT.repository.SubjectRepository;
import com.CBTServer.WebCSAT.service.ExamService;
import com.CBTServer.WebCSAT.service.QuestionService;
import com.CBTServer.WebCSAT.service.SubjectSubclassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // ✅ springframework
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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



    /** ✅ 3. 시험 제출 */
    @PostMapping("/submitExam")
    public String submitExam(@AuthenticationPrincipal User user,
                             @RequestParam Long subclassId,
                             @RequestParam String csatDate,
                             @ModelAttribute ExamDTO dto,
                             Model model) {

        // ✅ 더미 시험 제출 차단
        if (subclassId == 0 || "더미용".equals(csatDate)) {
            model.addAttribute("isDummy", true);  // 더미 플래그
            return "test/dummyBlocked";
        }

        Subclass subclass = subclassRepository.findById(subclassId)
                .orElseThrow(() -> new RuntimeException("부과목 없음: " + subclassId));

        List<SubmittedAnswerDTO> answerList = dto.toSubmittedAnswerList();
        Exam exam = examService.submitExam(user, subclass, csatDate, answerList);
        model.addAttribute("exam", exam);

        return "test/examResult";
    }

    @Autowired
    private SubjectRepository subjectRepository;


}


