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
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final SubclassRepository subclassRepository;
    private final CsatDateRepository csatDateRepository;
    private final ImageMetaRepository imageMetaRepository;
    private final SubclassCsatDateRepository subclassCsatDateRepository;

    public Question findById(Long questionId) {
        return questionRepository.findById(questionId).orElseThrow(() -> new IllegalArgumentException(questionId + " : 존재하지 않는 ID 입니다."));
    }

    public QuestionDTO getQuestion(Long questionId) {
        Question question = findById(questionId);
        return new QuestionDTO(question);
    }

    @Transactional
    public QuestionDTO newQuestion(String csatDate, Long subjectId, Long subclassId, int num) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow();
        CsatDate cD = csatDateRepository.findById(csatDate).orElseThrow();
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

    public QuestionDTO createBankQuestion(String csatDate, Long subjectId, Long subclassId, int num) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow();
        CsatDate cD = csatDateRepository.findById(csatDate).orElseThrow();
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

    public List<QuestionDTO> findFilteredQuestions(String csatDate, Long subjectId, Long subclassId) {
        return questionRepository.findAll().stream()
                .filter(q -> csatDate == null || (q.getCsatDate() != null && q.getCsatDate().getCsatDate().equals(csatDate)))
                .filter(q -> subjectId == null || (q.getSubject() != null && q.getSubject().getSubjectId().equals(subjectId)))
                .filter(q -> subclassId == null || (q.getSubclass() != null && q.getSubclass().getSubclassId().equals(subclassId)))
                .map(QuestionDTO::new)
                .toList();
    }

    @Transactional
    public void createMockQuestion(String csatDate, Long subjectId, Long subclassId) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow();
        CsatDate cD = csatDateRepository.findById(csatDate).orElseThrow();
        List<Subclass> subclassList = new ArrayList<>();
        if(subject.getOption() == 1) {
            List<Subclass> sL = subclassRepository.findAllBySubject_SubjectId(subjectId);
            for (Subclass s : sL) {
                if(!s.isOptional()) subclassList.add(s);
            }
        }
        subclassList.add(subclass);

        int globalNum = 1; // 과목 전체에서 공유되는 문제 번호

        for (Subclass s : subclassList) {
            int count = s.getCount(); // 해당 세부과목에서 생성할 문제 수
            Optional<SubclassCsatDate> sccd = subclassCsatDateRepository.findById_SubclassIdAndId_CsatDate(s.getSubclassId(),csatDate);
            if(sccd.isPresent()) {
                globalNum += count;
                continue;
            }
            SubclassCsatDateId id = new SubclassCsatDateId(s.getSubclassId(), cD.getCsatDate());
            SubclassCsatDate entity = new SubclassCsatDate();
            entity.setId(id);
            entity.setSubclass(s);
            entity.setCsatDate(cD);
            subclassCsatDateRepository.save(entity);
            for (int i = 0; i < count; i++) {
                Question question = Question.builder()
                        .subject(subject)
                        .subclass(s)
                        .csatDate(cD)
                        .num(globalNum++) // 1부터 전역적으로 증가
                        .questionType(true)
                        .build();
                questionRepository.save(question);
            }
        }
    }

    public void remove(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다."));
        questionRepository.delete(question);
    }
}
