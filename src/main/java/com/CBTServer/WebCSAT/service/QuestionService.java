package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubjectDTO;
import com.CBTServer.WebCSAT.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final SubclassRepository subclassRepository;
    private final CsatDateRepository csatDateRepository;
    private final ImageMetaRepository imageMetaRepository;

    public Question findById(Long questionId) {
        return questionRepository.findById(questionId).orElseThrow(() -> new IllegalArgumentException(questionId + " : 존재하지 않는 ID 입니다."));
    }

    public List<Question> getQuestions(Long subclassId, String csatDate) {
        CsatDate csatDateEntity = csatDateRepository.findById(csatDate)
                .orElseThrow(() -> new IllegalArgumentException("해당 csatDate 없음: " + csatDate));
        return questionRepository.findBySubclass_SubclassIdAndCsatDate(subclassId, csatDateEntity);
    }

    public QuestionDTO getQuestion(Long questionId) {
        Question question = findById(questionId);
        return new QuestionDTO(question);
    }

    @Transactional
    public QuestionDTO newQuestion(String csatDate, Long subjectId, Long subclassId, int num) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow();
        CsatDate cD = csatDateRepository.findById(csatDate).orElseThrow(()-> new IllegalArgumentException("이게 문제임"));
        Question question = Question.builder()
                .subject(subject)
                .subclass(subclass)
                .csatDate(cD)
                .num(num)
                .questionType(true)
                .build();
        Question newQuestion = questionRepository.save(question);
        return new QuestionDTO(newQuestion);
    }


    @Transactional
    public QuestionDTO updateQuestion(Long questionId, QuestionDTO questionDTO) {
        Question question = findById(questionId);

        // ⛳ ImageMeta 참조 수정
        if (questionDTO.getQuestionArticle() != null && !questionDTO.getQuestionArticle().isBlank()) {
            ImageMeta articleImage = imageMetaRepository.findById(questionDTO.getQuestionArticle())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 article 이미지 URL"));
            question.setQuestionArticle(articleImage);
        } else {
            question.setQuestionArticle(null);
        }

        if (questionDTO.getQuestionContext() != null && !questionDTO.getQuestionContext().isBlank()) {
            ImageMeta contextImage = imageMetaRepository.findById(questionDTO.getQuestionContext())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 context 이미지 URL"));
            question.setQuestionContext(contextImage);
        } else {
            question.setQuestionContext(null);
        }

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

    public List<QuestionDTO> findAllQuestion() {
        List<Question> questionList = questionRepository.findAll();
        List<QuestionDTO> dtoList = new ArrayList<>();
        for(Question q : questionList) {
            QuestionDTO dto = new QuestionDTO(q);
            dtoList.add(dto);
        }
        return dtoList;
    }
    public List<CsatDate> getAllCsatDates() {
        return csatDateRepository.findAll();
    }

    public List<String> getCsatDatesByType(String questionType) {
        List<CsatDate> allDates = questionRepository.findDistinctCsatDates();

        // CsatDate 객체에서 문자열을 추출
        List<String> dateStrings = allDates.stream()
                .map(CsatDate::getCsatDate)  // ← 여기서 csatDate(String) 값만 추출
                .toList();

        if ("모의고사".equals(questionType)) {
            return dateStrings.stream()
                    .filter(date -> date.matches("\\d{4}년\\d{1,2}월"))
                    .toList();
        } else {
            return dateStrings.stream()
                    .filter(date -> !date.matches("\\d{4}년\\d{1,2}월"))
                    .toList();
        }
    }

    public List<String> getAvailableCsatDatesBySubclass(Long subclassId) {
        List<Question> questions = questionRepository.findBySubclass_SubclassId(subclassId);

        return questions.stream()
                .map(q -> q.getCsatDate().getCsatDate())
                .filter(date -> date.matches("\\d{4}년\\d{1,2}월")) // ← 여기!
                .distinct()
                .sorted(Comparator.reverseOrder()) // 최신순 정렬 (선택)
                .collect(Collectors.toList());
    }



}
