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
}
