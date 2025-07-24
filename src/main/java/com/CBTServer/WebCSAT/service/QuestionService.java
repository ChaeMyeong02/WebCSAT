package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.*;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
    public void createMockQuestion(
            String csatDate,
            Long subjectId,
            Long subclassId,
            List<Integer> cuts, // cut1~cut9 입력 받기
            Map<Long, String> subclassListeningUrlMap // subclassId -> S3 mp3 URL 매핑
    ) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow();
        CsatDate cD = csatDateRepository.findById(csatDate).orElseThrow();

        List<Subclass> subclassList = new ArrayList<>();
        if (subject.getOption() == 1) {
            List<Subclass> sL = subclassRepository.findAllBySubject_SubjectId(subjectId);
            for (Subclass s : sL) {
                if (!s.isOptional()) subclassList.add(s);
            }
        }
        subclassList.add(subclass);

        int globalNum = 1;

        for (Subclass s : subclassList) {
            int count = s.getCount();
            Optional<SubclassCsatDate> existing = subclassCsatDateRepository.findById_SubclassIdAndId_CsatDate(s.getSubclassId(), csatDate);
            if (existing.isPresent()) {
                globalNum += count;
                continue;
            }

            SubclassCsatDateId id = new SubclassCsatDateId(s.getSubclassId(), cD.getCsatDate());
            SubclassCsatDate entity = new SubclassCsatDate();
            entity.setId(id);
            entity.setSubclass(s);
            entity.setCsatDate(cD);

            if(s.isOptional()) {
                // 상대평가면 입력받은 cuts 사용
                if (subject.isRelative()) {
                    if (cuts == null || cuts.size() != 9)
                        throw new IllegalArgumentException("등급컷은 1~9등급까지 모두 입력해야 합니다.");
                    entity.setCut1(cuts.get(0));
                    entity.setCut2(cuts.get(1));
                    entity.setCut3(cuts.get(2));
                    entity.setCut4(cuts.get(3));
                    entity.setCut5(cuts.get(4));
                    entity.setCut6(cuts.get(5));
                    entity.setCut7(cuts.get(6));
                    entity.setCut8(cuts.get(7));
                    entity.setCut9(cuts.get(8));
                } else {
                    // 절대평가면 고정값
                    entity.setCut1(90);
                    entity.setCut2(80);
                    entity.setCut3(70);
                    entity.setCut4(60);
                    entity.setCut5(50);
                    entity.setCut6(40);
                    entity.setCut7(30);
                    entity.setCut8(20);
                    entity.setCut9(10);
                }
            }

            // 듣기 과목이면 URL 지정
            if (s.isListening()) {
                String url = subclassListeningUrlMap.get(s.getSubclassId());
                if (url == null || url.isBlank()) {
                    throw new IllegalArgumentException("듣기 과목에는 반드시 MP3를 지정해야 합니다.");
                }
                entity.setListeningUrl(url);
            }

            subclassCsatDateRepository.save(entity);

            for (int i = 0; i < count; i++) {
                Question question = Question.builder()
                        .subject(subject)
                        .subclass(s)
                        .csatDate(cD)
                        .num(globalNum++)
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

    public List<Question> getQuestions(Long subclassId, String csatDate) {
        CsatDate csatDateEntity = csatDateRepository.findById(csatDate)
                .orElseThrow(() -> new IllegalArgumentException("해당 csatDate 없음: " + csatDate));
        return questionRepository.findBySubclass_SubclassIdAndCsatDate(subclassId, csatDateEntity);
    }

}
