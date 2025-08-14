package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface LookupRepository extends JpaRepository<Subject, Long> {
    interface IdName { Long getId(); String getName(); }

    @Query(value = "SELECT DISTINCT scd.csat_date FROM subclass_csat_date scd ORDER BY scd.csat_date", nativeQuery = true)
    List<String> findAllCsatDates();

    @Query(value = "SELECT s.subject_id AS id, s.subject_name AS name " +
            "FROM subject s " +
            "ORDER BY s.subject_id",                // ✅ 과목명 오름차순
            nativeQuery = true)
    List<IdName> findSubjects();

    // LookupRepository.java (정렬 유지)
    @Query(value = "SELECT sc.subclass_id AS id, sc.subclass_name AS name " +
            "FROM subclass sc " +
            "WHERE sc.subject_id = :subjectId " +
            "  AND sc.optional = b'1' " +               // ✅ 선택과목만 노출
            "ORDER BY sc.subclass_id", nativeQuery = true)
    List<IdName> findSubclasses(@Param("subjectId") Long subjectId);

    // 그대로 OK
    @Query(value = """
      SELECT scd.csat_date
      FROM subclass_csat_date scd
      WHERE scd.subclass_id = :subclassId
      ORDER BY scd.csat_date
      """, nativeQuery = true)
    List<String> findCsatDatesForSubclass(@Param("subclassId") Long subclassId);

    @Query(value = """
      SELECT e.exam_id,
             e.subclass_id,
             e.csat_date,
             e.test_date,
             CASE
               WHEN e.cnt <= 1 THEN 50
               ELSE CAST(ROUND((e.rk - 1) / (e.cnt - 1) * 100) AS SIGNED)
             END AS percentile
      FROM (
          SELECT ex.exam_id, ex.user_id, ex.subclass_id, ex.csat_date, ex.test_date, ex.result,
                 RANK()  OVER (PARTITION BY ex.csat_date, ex.subclass_id ORDER BY ex.result) AS rk,
                 COUNT(*) OVER (PARTITION BY ex.csat_date, ex.subclass_id)                   AS cnt
          FROM exam ex
      ) e
      JOIN subclass sc ON sc.subclass_id = e.subclass_id
      WHERE e.user_id = :uid
        AND (:subjectId  IS NULL OR sc.subject_id  = :subjectId)
        AND (:subclassId IS NULL OR e.subclass_id  = :subclassId)
        AND (:csatDate   IS NULL OR e.csat_date    = :csatDate)
      ORDER BY e.test_date ASC
      """, nativeQuery = true)
        List<Object[]> attemptTrend(
                @Param("uid") Long uid,
                @Param("subjectId") Long subjectId,
                @Param("subclassId") Long subclassId,
                @Param("csatDate") String csatDate
        );


    @Query(value = """
    SELECT e.exam_id, e.subclass_id, e.csat_date, e.test_date, e.result
    FROM exam e
    LEFT JOIN subclass sc ON sc.subclass_id = e.subclass_id
    WHERE e.user_id = :uid
      AND (:subjectId IS NULL OR sc.subject_id = :subjectId)
      AND (:subclassId IS NULL OR e.subclass_id = :subclassId)
      AND (:csatDate  IS NULL OR e.csat_date = :csatDate)
    ORDER BY e.test_date ASC
  """, nativeQuery = true)
    List<Object[]> attemptTrendUser(
            @Param("uid") Long uid,
            @Param("subjectId") Long subjectId,
            @Param("subclassId") Long subclassId,
            @Param("csatDate") String csatDate
    );

    @Query(value = """
    SELECT NULL              AS exam_id,
           COALESCE(e.subclass_id, :subclassId) AS subclass_id,
           e.csat_date       AS csat_date,
           MIN(e.test_date)  AS test_date,
           AVG(e.result)     AS result
    FROM exam e
    LEFT JOIN subclass sc ON sc.subclass_id = e.subclass_id
    WHERE (:subjectId IS NULL OR sc.subject_id = :subjectId)
      AND (:subclassId IS NULL OR e.subclass_id = :subclassId)
      AND (:csatDate  IS NULL OR e.csat_date = :csatDate)
    GROUP BY e.csat_date, COALESCE(e.subclass_id, :subclassId)
    ORDER BY MIN(e.test_date) ASC
  """, nativeQuery = true)
    List<Object[]> attemptTrendAdmin(
            @Param("subjectId") Long subjectId,
            @Param("subclassId") Long subclassId,
            @Param("csatDate") String csatDate
    );
}

