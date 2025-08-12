package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import com.CBTServer.WebCSAT.repository.proj.AttemptRow;
import com.CBTServer.WebCSAT.repository.proj.AttemptRow;
import com.CBTServer.WebCSAT.repository.proj.BinRow;
import com.CBTServer.WebCSAT.repository.proj.CohortCountRow;
import com.CBTServer.WebCSAT.repository.proj.UserLatestAttemptRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<Exam, Long> {

    // ──[ Cohort 범위 해석용 ]───────────────────────────────────────────────
    @Query(value = "SELECT sc.subclass_id FROM subclass sc", nativeQuery = true)
    List<Long> findAllSubclassIds();

    @Query(value = "SELECT sc.subclass_id FROM subclass sc WHERE sc.subject_id = :subjectId", nativeQuery = true)
    List<Long> findSubclassIdsBySubject(@Param("subjectId") Long subjectId);

    @Query(value = "SELECT s.subject_id FROM subclass s WHERE s.subclass_id = :subclassId", nativeQuery = true)
    Long findSubjectIdBySubclass(@Param("subclassId") Long subclassId);

    // optional=0 → 공통과목
    @Query(value = "SELECT s.subclass_id FROM subclass s WHERE s.subject_id = :subjectId AND s.optional = b'0'", nativeQuery = true)
    List<Long> findCommonSubclassIds(@Param("subjectId") Long subjectId);

    // ──[ 분포/집계 : 다중 subclass(ALL/SUBJECT/묶음) ]────────────────────────
    @Query(value = """
        SELECT FLOOR(e.result / :binSize) AS bin, COUNT(*) AS cnt
        FROM exam e
        WHERE e.csat_date = :csatDate AND e.subclass_id IN (:subclassIds)
        GROUP BY bin ORDER BY bin
        """, nativeQuery = true)
    List<BinRow> buildHistogramIn(@Param("csatDate") String csatDate,
                                  @Param("subclassIds") List<Long> subclassIds,
                                  @Param("binSize") int binSize);

    @Query(value = "SELECT COUNT(DISTINCT e.user_id) FROM exam e WHERE e.csat_date = :csatDate AND e.subclass_id IN (:subclassIds)", nativeQuery = true)
    int countParticipantsIn(@Param("csatDate") String csatDate,
                            @Param("subclassIds") List<Long> subclassIds);

    @Query(value = "SELECT COUNT(*) FROM exam e WHERE e.csat_date = :csatDate AND e.subclass_id IN (:subclassIds)", nativeQuery = true)
    int countAttemptsIn(@Param("csatDate") String csatDate,
                        @Param("subclassIds") List<Long> subclassIds);

    @Query(value = "SELECT e.result FROM exam e WHERE e.csat_date = :csatDate AND e.subclass_id IN (:subclassIds) ORDER BY e.result", nativeQuery = true)
    List<Integer> findSortedScoresIn(@Param("csatDate") String csatDate,
                                     @Param("subclassIds") List<Long> subclassIds);

    @Query(value = """
        SELECT SUM(CASE WHEN e.result < :score THEN 1 ELSE 0 END) AS lessCnt,
               COUNT(*) AS cohort
        FROM exam e
        WHERE e.csat_date = :csatDate AND e.subclass_id IN (:subclassIds)
        """, nativeQuery = true)
    CohortCountRow countCohortAndLessIn(@Param("csatDate") String csatDate,
                                        @Param("subclassIds") List<Long> subclassIds,
                                        @Param("score") Integer score);

    // ──[ 분포/집계 : 단일 subclass(SUBCLASS scope 호환) ]──────────────────────
    @Query(value = """
        SELECT FLOOR(e.result / :binSize) AS bin, COUNT(*) AS cnt
        FROM exam e
        WHERE e.csat_date = :csatDate AND e.subclass_id = :subclassId
        GROUP BY bin ORDER BY bin
        """, nativeQuery = true)
    List<BinRow> buildHistogram(@Param("csatDate") String csatDate,
                                @Param("subclassId") Long subclassId,
                                @Param("binSize") int binSize);

    @Query(value = "SELECT COUNT(DISTINCT e.user_id) FROM exam e WHERE e.csat_date = :csatDate AND e.subclass_id = :subclassId", nativeQuery = true)
    int countParticipants(@Param("csatDate") String csatDate,
                          @Param("subclassId") Long subclassId);

    @Query(value = "SELECT COUNT(*) FROM exam e WHERE e.csat_date = :csatDate AND e.subclass_id = :subclassId", nativeQuery = true)
    int countAttempts(@Param("csatDate") String csatDate,
                      @Param("subclassId") Long subclassId);

    @Query(value = "SELECT e.result FROM exam e WHERE e.csat_date = :csatDate AND e.subclass_id = :subclassId ORDER BY e.result", nativeQuery = true)
    List<Integer> findSortedScores(@Param("csatDate") String csatDate,
                                   @Param("subclassId") Long subclassId);

    @Query(value = """
        SELECT SUM(CASE WHEN e.result < :score THEN 1 ELSE 0 END) AS lessCnt,
               COUNT(*) AS cohort
        FROM exam e
        WHERE e.csat_date = :csatDate AND e.subclass_id = :subclassId
        """, nativeQuery = true)
    CohortCountRow countCohortAndLess(@Param("csatDate") String csatDate,
                                      @Param("subclassId") Long subclassId,
                                      @Param("score") Integer score);

    @Query(value = """
        SELECT scd.cut1
        FROM subclass_csat_date scd
        WHERE scd.csat_date = :csatDate AND scd.subclass_id = :subclassId
        """, nativeQuery = true)
    Integer findGrade1Cut(@Param("csatDate") String csatDate,
                          @Param("subclassId") Long subclassId);

    // ──[ 사용자 개별 ]──────────────────────────────────────────────────────
    @Query(value = """
        SELECT e.result AS score
        FROM exam e
        WHERE e.user_id = :userId AND e.csat_date = :csatDate AND e.subclass_id = :subclassId
        ORDER BY e.test_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Integer findMyLatestScore(@Param("userId") Long userId,
                              @Param("csatDate") String csatDate,
                              @Param("subclassId") Long subclassId);

    @Query(value = """
        SELECT e.exam_id AS examId,
               e.csat_date AS csatDate,
               e.subclass_id AS subclassId,
               e.result AS score,
               e.test_date AS testDate
        FROM exam e
        JOIN (
            SELECT csat_date, subclass_id, MAX(test_date) AS max_date
            FROM exam
            WHERE user_id = :userId
            GROUP BY csat_date, subclass_id
        ) t ON t.csat_date = e.csat_date
           AND t.subclass_id = e.subclass_id
           AND t.max_date = e.test_date
        WHERE e.user_id = :userId
        ORDER BY e.test_date ASC
        """, nativeQuery = true)
    List<UserLatestAttemptRow> findUserLatestAttemptsGrouped(@Param("userId") Long userId);

    // ──[ 관리자용 (회차 전체) ]──────────────────────────────────────────────
    @Query(value = "SELECT COUNT(DISTINCT e.user_id) FROM exam e WHERE e.csat_date = :csatDate", nativeQuery = true)
    int countParticipantsAllSubjects(@Param("csatDate") String csatDate);

    @Query(value = """
        SELECT COUNT(*) FROM (
          SELECT e.user_id
          FROM exam e
          WHERE e.csat_date = :csatDate
          GROUP BY e.user_id
          HAVING COUNT(*) >= 2
        ) z
        """, nativeQuery = true)
    int countReattemptUsers(@Param("csatDate") String csatDate);

    @Query(value = """
        SELECT AVG(cnt) FROM (
          SELECT COUNT(*) AS cnt
          FROM exam e
          WHERE e.csat_date = :csatDate
          GROUP BY e.user_id
        ) z
        """, nativeQuery = true)
    Double avgAttemptsPerUser(@Param("csatDate") String csatDate);

    @Query(value = """
  SELECT 
    e.exam_id        AS examId,
    e.csat_date      AS csatDate,
    e.subclass_id    AS subclassId,
    sc.name          AS subclassName,
    e.test_date      AS testDate,
    e.result         AS score
  FROM exam e
  JOIN subclass sc ON sc.subclass_id = e.subclass_id
  WHERE e.user_id = :userId
    AND (:csatDate  IS NULL OR e.csat_date   = :csatDate)
    AND (:subjectId IS NULL OR sc.subject_id = :subjectId)
    AND (:subclassId IS NULL OR e.subclass_id = :subclassId)
  ORDER BY e.test_date DESC
""", nativeQuery = true)
    List<AttemptRow> findAttempts(@Param("userId") Long userId,
                                  @Param("csatDate") String csatDate,
                                  @Param("subjectId") Long subjectId,
                                  @Param("subclassId") Long subclassId);
}
