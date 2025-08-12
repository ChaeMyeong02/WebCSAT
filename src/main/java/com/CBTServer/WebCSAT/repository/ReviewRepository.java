package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Exam, Long> {
    @Query(value = """
        SELECT 
            e.exam_id        AS examId,
            e.test_date      AS testDate,
            e.csat_date      AS csatDate,
            sc.name          AS subclassName,
            e.result         AS score
        FROM exam e
        JOIN subclass sc ON sc.subclass_id = e.subclass_id
        WHERE e.user_id = :userId
          AND (:csatDate  IS NULL OR e.csat_date  = :csatDate)
          AND (:subjectId IS NULL OR sc.subject_id = :subjectId)
          AND (:subclassId IS NULL OR e.subclass_id = :subclassId)
        ORDER BY e.test_date DESC
        """, nativeQuery = true)
    List<com.CBTServer.WebCSAT.repository.proj.AttemptRow> findAttempts(
            @Param("userId") Long userId,
            @Param("csatDate") String csatDate,
            @Param("subjectId") Long subjectId,
            @Param("subclassId") Long subclassId
    );

    // ⬇️ 네 스키마에 맞게 table/column 이름만 확인해서 바꿔줘
    @Query(value = """
        SELECT e.exam_id   AS examId,
               e.csat_date AS csatDate,
               e.subclass_id AS subclassId,
               sc.name      AS subclassName,
               DATE_FORMAT(e.test_date, '%Y-%m-%d %H:%i:%s') AS testDate,
               e.result     AS score
        FROM exam e
        JOIN subclass sc ON sc.subclass_id = e.subclass_id
        WHERE e.user_id = :userId
          AND (:csatDate  IS NULL OR e.csat_date   = :csatDate)
          AND (:subjectId IS NULL OR sc.subject_id = :subjectId)
          AND (:subclassId IS NULL OR e.subclass_id = :subclassId)
        ORDER BY e.test_date DESC
        """, nativeQuery = true)
    List<AttemptRow> findAttemptSummaries(@Param("userId") Long userId,
                                          @Param("csatDate") String csatDate,
                                          @Param("subjectId") Long subjectId,
                                          @Param("subclassId") Long subclassId);

    interface AttemptRow {
        Long getExamId();
        String getCsatDate();
        Long getSubclassId();
        String getSubclassName();
        String getTestDate();
        Integer getScore();
    }

    // 상세(복기) 쿼리 — ⚠️ 네 스키마에 맞게 테이블명/컬럼명만 교체
    @Query(value = """
        SELECT
          q.question_id        AS questionId,
          q.number             AS number,
          q.content            AS questionContent,
          qc.choice_id         AS choiceId,
          qc.label             AS choiceLabel,
          qc.content           AS choiceContent,
          q.correct_choice_id  AS correctChoiceId,
          ea.choice_id         AS selectedChoiceId
        FROM exam_question eq
        JOIN question q          ON q.question_id = eq.question_id
        JOIN question_choice qc  ON qc.question_id = q.question_id
        LEFT JOIN exam_answer ea ON ea.exam_id = eq.exam_id AND ea.question_id = q.question_id
        WHERE eq.exam_id = :examId
        ORDER BY q.number, qc.label
        """, nativeQuery = true)
    List<ReviewRow> fetchReviewRows(@Param("examId") Long examId);

    interface ReviewRow {
        Long getQuestionId();
        Integer getNumber();
        String getQuestionContent();
        Long getChoiceId();
        String getChoiceLabel();
        String getChoiceContent();
        Long getCorrectChoiceId();
        Long getSelectedChoiceId();
    }
}
