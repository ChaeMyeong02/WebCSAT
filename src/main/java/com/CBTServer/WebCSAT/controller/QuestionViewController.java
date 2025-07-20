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
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class QuestionViewController {
    private final SubjectSubclassService subjectSubclassService;
    private final QuestionService questionService;
    private final CsatDateService csatDateService;

    @GetMapping("/admin/addTestQuestion")
    public String addTestQuestion(Model model1, Model model2) {
        List<SubjectDTO> subjectDTOList = subjectSubclassService.findAllSubject();
        model1.addAttribute(subjectDTOList);
        List<SubclassDTO> subclassDTOList = subjectSubclassService.findAllSubclass();
        model2.addAttribute(subclassDTOList);
        return "addTestQuestion";
    }

    @GetMapping("/admin/questionList")
    public String questionList(Model model) {
        List<QuestionDTO> questionDTOList = questionService.findAllQuestion();
        List<CsatDate> csatDateList = csatDateService.findAllDate();

        // 과목명 매핑
        Map<Long, String> subjectNameMap = subjectSubclassService.findAllSubject().stream()
                .collect(Collectors.toMap(SubjectDTO::getSubjectId, SubjectDTO::getSubjectName));

        // 세부과목명 매핑
        Map<Long, String> subclassNameMap = subjectSubclassService.findAllSubclass().stream()
                .collect(Collectors.toMap(SubclassDTO::getSubclassId, SubclassDTO::getSubclassName));

        model.addAttribute("questionDTOList", questionDTOList);
        model.addAttribute("subjectNameMap", subjectNameMap);
        model.addAttribute("subclassNameMap", subclassNameMap);
        model.addAttribute("csatDateList", csatDateList);

        return "questionList";
    }




    /* Admin */
    @GetMapping("/admin/editQuestion")
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
