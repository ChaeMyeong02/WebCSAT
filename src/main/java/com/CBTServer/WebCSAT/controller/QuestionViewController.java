package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubclassDTO;
import com.CBTServer.WebCSAT.dto.SubjectDTO;
import com.CBTServer.WebCSAT.service.CsatDateService;
import com.CBTServer.WebCSAT.service.QuestionService;
import com.CBTServer.WebCSAT.service.SubjectSubclassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class QuestionViewController {
    private final SubjectSubclassService subjectSubclassService;
    private final QuestionService questionService;
    private final CsatDateService csatDateService;

    @GetMapping("/admin/question/add")
    public String addTestQuestion(Model model1, Model model2) {
        List<SubjectDTO> subjectDTOList = subjectSubclassService.findAllSubject();
        model1.addAttribute(subjectDTOList);
        List<SubclassDTO> subclassDTOList = subjectSubclassService.findAllSubclass();
        model2.addAttribute(subclassDTOList);
        List<CsatDate> csatDateList = csatDateService.findAllDate();
        Optional<CsatDate> remove = csatDateService.findByDate("문제은행용");
        CsatDate r = (remove.isPresent()) ? remove.get() : null;
        csatDateList.remove(r);
        model2.addAttribute(csatDateList);
        return "addTestQuestion";
    }



    /* Admin */
    @GetMapping("/admin/question/list")
    public String questionList(
            @RequestParam(value = "csatDate", required = false) String csatDate,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "subclassId", required = false) Long subclassId,
            Model model) {

        // 조건에 맞는 문제 조회
        List<QuestionDTO> questionDTOList = questionService.findFilteredQuestions(csatDate, subjectId, subclassId);

        // 날짜 필터용 전체 목록
        List<CsatDate> csatDateList = csatDateService.findAllDate();

        // 과목명/세부과목명 매핑
        Map<Long, String> subjectNameMap = subjectSubclassService.findAllSubject().stream()
                .collect(Collectors.toMap(SubjectDTO::getSubjectId, SubjectDTO::getSubjectName));

        Map<Long, String> subclassNameMap = subjectSubclassService.findAllSubclass().stream()
                .collect(Collectors.toMap(SubclassDTO::getSubclassId, SubclassDTO::getSubclassName));

        model.addAttribute("questionDTOList", questionDTOList);
        model.addAttribute("subjectNameMap", subjectNameMap);
        model.addAttribute("subclassNameMap", subclassNameMap);
        model.addAttribute("csatDateList", csatDateList);

        // 필터 유지를 위한 파라미터도 모델에 포함
        model.addAttribute("selectedDate", csatDate);
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedSubclassId", subclassId);

        return "questionList";
    }



    @GetMapping("/admin/question/edit")
    public String editQuestion(@RequestParam("questionId") Long questionId, Model model) {
        Question question = questionService.findById(questionId);
        QuestionDTO questionDTO = new QuestionDTO(question);
        model.addAttribute(questionDTO);
        // 과목명 매핑
        Map<Long, String> subjectNameMap = subjectSubclassService.findAllSubject().stream()
                .collect(Collectors.toMap(SubjectDTO::getSubjectId, SubjectDTO::getSubjectName));

        // 세부과목명 매핑
        Map<Long, String> subclassNameMap = subjectSubclassService.findAllSubclass().stream()
                .collect(Collectors.toMap(SubclassDTO::getSubclassId, SubclassDTO::getSubclassName));
        model.addAttribute("questionDTO", questionDTO);
        model.addAttribute("subjectNameMap", subjectNameMap);
        model.addAttribute("subclassNameMap", subclassNameMap);
        return "editQuestion";
    }
}
