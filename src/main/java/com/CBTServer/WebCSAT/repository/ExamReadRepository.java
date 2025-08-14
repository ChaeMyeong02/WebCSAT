// src/main/java/com/CBTServer/WebCSAT/repository/ExamReadRepository.java
package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;   // ✅ 이걸로 받자
import java.util.List;

@Repository
public interface ExamReadRepository extends JpaRepository<Exam, Long> {

    interface ExamRow {
        Long getExamId();
        Long getUserId();
        Long getSubclassId();
        String getCsatDate();          // csat_date가 VARCHAR라면 그대로 String
        Integer getResult();
        LocalDateTime getTestDate();   // ✅ String -> LocalDateTime (또는 Timestamp)
        String getSubclassName();
    }

    @Query(value = """
      SELECT 
        e.exam_id     AS examId,
        e.user_id     AS userId,
        e.subclass_id AS subclassId,
        e.csat_date   AS csatDate,
        e.result      AS result,
        e.test_date   AS testDate,
        sc.subclass_name  AS subclassName
      FROM exam e
      JOIN subclass sc ON sc.subclass_id = e.subclass_id
      WHERE (:csatDate  IS NULL OR e.csat_date   = :csatDate)
        AND (:subjectId IS NULL OR sc.subject_id = :subjectId)
        AND (:subclassId IS NULL OR e.subclass_id = :subclassId)
      ORDER BY e.test_date DESC
      """, nativeQuery = true)
    List<ExamRow> findAllFiltered(
            @Param("csatDate")   String csatDate,
            @Param("subjectId")  Long subjectId,
            @Param("subclassId") Long subclassId
    );
}
