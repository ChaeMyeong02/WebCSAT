package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.Exam;
import com.CBTServer.WebCSAT.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository  extends JpaRepository<Question, Long> {
    @Query("""
    SELECT COUNT(q) > 0
    FROM Question q
    WHERE q.questionArticle.url = :imageUrl
       OR q.questionContext.url = :imageUrl
    """)
    boolean isImageUrlUsedAnywhere(@Param("imageUrl") String imageUrl);

    List<Question> findBySubclass_SubclassIdAndCsatDate(Long subclassId, CsatDate csatDate);

    List<Question> findFirst20ByOrderByQuestionIdAsc();

    @Query("SELECT DISTINCT q.csatDate FROM Question q")
    List<CsatDate> findDistinctCsatDates();

    List<Question> findBySubclass_SubclassId(Long subclassId);

    @Query("SELECT q FROM Question q WHERE q.subclass.subclassId = :subclassId AND q.csatDate.csatDate = :csatDate")
    List<Question> findBySubclassIdAndCsatDateString(@Param("subclassId") Long subclassId, @Param("csatDate") String csatDate);

    List<Question> findBySubclass_SubclassIdAndCsatDateOrderByNumAsc(Long subclassId, CsatDate csatDate);

    // 🔹🔹🔹 [추가] 만점(총점) 합계용 쿼리들 — SUM은 Long로 받는 게 안전합니다
    @Query("""
    SELECT SUM(COALESCE(q.score, 0))
    FROM Question q
    WHERE q.subclass.subclassId = :subclassId
      AND q.csatDate.csatDate = :csatDate
    """)
    Long sumScoreBySubclassIdAndCsatDate(@Param("subclassId") Long subclassId, @Param("csatDate") String csatDate);

    @Query("""
    SELECT SUM(COALESCE(q.score, 0))
    FROM Question q
    WHERE q.subclass.subclassId = :subclassId
    """)
    Long sumScoreBySubclassId(@Param("subclassId") Long subclassId);

    @Query("""
    SELECT SUM(COALESCE(q.score, 0))
    FROM Question q
    WHERE q.subject.subjectId = :subjectId
      AND q.csatDate.csatDate = :csatDate
    """)
    Long sumScoreBySubjectIdAndCsatDate(@Param("subjectId") Long subjectId, @Param("csatDate") String csatDate);

    @Query("""
    SELECT SUM(COALESCE(q.score, 0))
    FROM Question q
    WHERE q.subject.subjectId = :subjectId
    """)
    Long sumScoreBySubjectId(@Param("subjectId") Long subjectId);

    // 🔹🔹🔹 [추가] 세부과목별 사용 가능한 회차(문자열) 목록
    @Query("""
    SELECT DISTINCT q.csatDate.csatDate
    FROM Question q
    WHERE q.subclass.subclassId = :subclassId
    ORDER BY q.csatDate.csatDate DESC
    """)
    List<String> findDistinctCsatDates(@Param("subclassId") Long subclassId);

    @Query(value = """
  select coalesce(sum(q.score),0) from question q
  join subclass s on s.subclass_id = q.subclass_id
  where q.csat_date = :csatDate
    and (
         (s.subject_id = :subjectId and s.optional = 0)
      or (:subclassId is not null and q.subclass_id = :subclassId and s.optional = 1)
    )
""", nativeQuery = true)
    Integer sumPaperScore(@Param("csatDate") String csatDate,
                          @Param("subjectId") Long subjectId,
                          @Param("subclassId") Long subclassId);
}

