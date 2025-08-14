// src/main/java/com/CBTServer/WebCSAT/repository/ReviewQueryRepository.java
package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.dto.ExamReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepository {
    private final JdbcTemplate jdbc;

    /* ================== 기존(소유자 검증) ================== */
    @Transactional(readOnly = true)
    public ExamReviewDTO loadMeta(Long examId, Long userId) {
        return jdbc.queryForObject(
                """
                SELECT e.exam_id, e.csat_date, e.result, e.test_date,
                       sc.subclass_id, sc.subclass_name
                  FROM exam e
                  JOIN subclass sc ON sc.subclass_id = e.subclass_id
                 WHERE e.exam_id = ? AND e.user_id = ?
                """,
                (rs, rn) -> {
                    ExamReviewDTO d = new ExamReviewDTO();
                    d.setExamId(rs.getLong("exam_id"));
                    d.setCsatDate(rs.getString("csat_date"));
                    d.setResult((Integer) rs.getObject("result"));
                    var ts = rs.getTimestamp("test_date");
                    d.setTestDate(ts != null ? ts.toLocalDateTime() : null);
                    d.setSubclassId(rs.getLong("subclass_id"));
                    d.setSubclassName(rs.getString("subclass_name"));
                    return d;
                },
                examId, userId
        );
    }

    @Transactional(readOnly = true)
    public List<ExamReviewDTO.Item> loadItems(Long examId, Long userId) {
        return jdbc.query(
                """
                SELECT q.question_id, q.num, q.question_title,
                       q.option1, q.option2, q.option3, q.option4, q.option5,
                       q.answer, q.question_context,
                       eq.selected_answer AS user_answer
                  FROM exam_question eq
                  JOIN question q ON q.question_id = eq.question_id
                  JOIN exam e ON e.exam_id = eq.exam_id
                 WHERE eq.exam_id = ? AND e.user_id = ?
                 ORDER BY q.num ASC
                """,
                (rs, rn) -> {
                    ExamReviewDTO.Item it = new ExamReviewDTO.Item();
                    it.setId(rs.getLong("question_id"));
                    it.setNumber((Integer) rs.getObject("num"));
                    it.setStem(rs.getString("question_title"));
                    it.setOptions(Arrays.asList(
                            rs.getString("option1"),
                            rs.getString("option2"),
                            rs.getString("option3"),
                            rs.getString("option4"),
                            rs.getString("option5")
                    ));
                    it.setCorrect(rs.getString("answer"));       // 'A'..'E'
                    it.setMyAnswer(rs.getString("user_answer")); // 'A'..'E'
                    it.setExplanation(rs.getString("question_context"));
                    return it;
                },
                examId, userId
        );
    }

    /* ================== 관리자용(소유자 검증 없음) ================== */
    @Transactional(readOnly = true)
    public ExamReviewDTO loadMetaByExam(Long examId) {
        return jdbc.queryForObject(
                """
                SELECT e.exam_id, e.csat_date, e.result, e.test_date,
                       sc.subclass_id, sc.subclass_name
                  FROM exam e
                  JOIN subclass sc ON sc.subclass_id = e.subclass_id
                 WHERE e.exam_id = ?
                """,
                (rs, rn) -> {
                    ExamReviewDTO d = new ExamReviewDTO();
                    d.setExamId(rs.getLong("exam_id"));
                    d.setCsatDate(rs.getString("csat_date"));
                    d.setResult((Integer) rs.getObject("result"));
                    var ts = rs.getTimestamp("test_date");
                    d.setTestDate(ts != null ? ts.toLocalDateTime() : null);
                    d.setSubclassId(rs.getLong("subclass_id"));
                    d.setSubclassName(rs.getString("subclass_name"));
                    return d;
                },
                examId
        );
    }

    @Transactional(readOnly = true)
    public List<ExamReviewDTO.Item> loadItemsForExam(Long examId) {
        return jdbc.query(
                """
                SELECT q.question_id, q.num, q.question_title,
                       q.option1, q.option2, q.option3, q.option4, q.option5,
                       q.answer, q.question_context,
                       eq.selected_answer AS user_answer
                  FROM exam_question eq
                  JOIN question q ON q.question_id = eq.question_id
                 WHERE eq.exam_id = ?
                 ORDER BY q.num ASC
                """,
                (rs, rn) -> {
                    ExamReviewDTO.Item it = new ExamReviewDTO.Item();
                    it.setId(rs.getLong("question_id"));
                    it.setNumber((Integer) rs.getObject("num"));
                    it.setStem(rs.getString("question_title"));
                    it.setOptions(Arrays.asList(
                            rs.getString("option1"),
                            rs.getString("option2"),
                            rs.getString("option3"),
                            rs.getString("option4"),
                            rs.getString("option5")
                    ));
                    it.setCorrect(rs.getString("answer"));
                    it.setMyAnswer(rs.getString("user_answer"));
                    it.setExplanation(rs.getString("question_context"));
                    return it;
                },
                examId
        );
    }
}
