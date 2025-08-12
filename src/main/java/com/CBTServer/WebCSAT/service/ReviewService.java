package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.dto.*;
import com.CBTServer.WebCSAT.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository repo;

    public List<ReviewAttemptSummaryDTO> listAttempts(Long userId, String csatDate,
                                                      Long subjectId, Long subclassId) {
        var rows = repo.findAttemptSummaries(userId, csatDate, subjectId, subclassId);
        List<ReviewAttemptSummaryDTO> out = new ArrayList<>();
        for (var r : rows) {
            out.add(ReviewAttemptSummaryDTO.builder()
                    .examId(r.getExamId())
                    .csatDate(r.getCsatDate())
                    .subclassId(r.getSubclassId())
                    .subclassName(r.getSubclassName())
                    .testDate(r.getTestDate())
                    .score(r.getScore())
                    .build());
        }
        return out;
    }

    public ReviewAttemptDTO getAttemptDetail(Long examId) {
        var rows = repo.fetchReviewRows(examId);
        if (rows.isEmpty()) {
            return ReviewAttemptDTO.builder()
                    .examId(examId)
                    .questions(List.of())
                    .build();
        }
        Map<Long, ReviewQuestionDTO> qmap = new LinkedHashMap<>();
        String csat = null; Long scId = null; String scName = null; // 있으면 채우고, 없으면 생략

        for (var r : rows) {
            var q = qmap.computeIfAbsent(r.getQuestionId(), id ->
                    ReviewQuestionDTO.builder()
                            .questionId(r.getQuestionId())
                            .number(Optional.ofNullable(r.getNumber()).orElse(0))
                            .content(r.getQuestionContent())
                            .choices(new ArrayList<>())
                            .selectedChoiceId(r.getSelectedChoiceId())
                            .correctChoiceId(r.getCorrectChoiceId())
                            .correct(Objects.equals(r.getSelectedChoiceId(), r.getCorrectChoiceId()))
                            .build()
            );
            ((List<ReviewChoiceDTO>) q.getChoices()).add(
                    ReviewChoiceDTO.builder()
                            .choiceId(r.getChoiceId())
                            .label(r.getChoiceLabel())
                            .content(r.getChoiceContent())
                            .build()
            );
        }

        var questions = new ArrayList<>(qmap.values());
        questions.sort(Comparator.comparingInt(ReviewQuestionDTO::getNumber));
        questions.forEach(q -> q.getChoices().sort(Comparator.comparing(ReviewChoiceDTO::getLabel)));

        return ReviewAttemptDTO.builder()
                .examId(examId)
                .csatDate(csat)
                .subclassId(scId)
                .subclassName(scName)
                .questions(questions)
                .build();
    }
}
