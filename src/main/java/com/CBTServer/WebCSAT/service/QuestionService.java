package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.domain.Subject;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import com.CBTServer.WebCSAT.repository.QuestionRepository;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import com.CBTServer.WebCSAT.repository.SubjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final SubclassRepository subclassRepository;
    private final CsatDateRepository csatDateRepository;

    public Question findById(Long questionId) {
        return questionRepository.findById(questionId).orElseThrow(() -> new IllegalArgumentException(questionId + " : 존재하지 않는 ID 입니다."));
    }

    public QuestionDTO getQuestion(Long questionId) {
        Question question = findById(questionId);
        return new QuestionDTO(question);
    }

    @Transactional
    public QuestionDTO newQuestion(String castDateStr, Long subjectId, Long subclassId, int num, boolean questionType) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow();
        CsatDate csatDate = csatDateRepository.findById(castDateStr).orElseThrow();
        Question question = Question.builder()
                .subject(subject)
                .subclass(subclass)
                .csatDate(csatDate)
                .num(num)
                .questionType(questionType)
                .build();
        Question newQuestion = questionRepository.save(question);
        return new QuestionDTO(newQuestion);
    }


    @Transactional
    public QuestionDTO updateQuestion(Long questionId, QuestionDTO questionDTO) {
        Question question = questionRepository.findById(questionId).orElseThrow(()->new IllegalArgumentException(questionId + " : 존재하지 않는 ID 입니다."));
        question.setQuestionContext(questionDTO.getQuestionContext());
        question.setAnswer(questionDTO.getAnswer());
        question.setOption1(questionDTO.getOption1());
        question.setOption2(questionDTO.getOption2());
        question.setOption3(questionDTO.getOption3());
        question.setOption4(questionDTO.getOption4());
        question.setOption5(questionDTO.getOption5());
        question.setQuestionType(questionDTO.isQuestionType());
        question.setQuestionTitle(questionDTO.getQuestionTitle());
        question.setScore(questionDTO.getScore());
        return new QuestionDTO(question);
    }
}
