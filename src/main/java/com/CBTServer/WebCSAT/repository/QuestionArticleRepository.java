package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.QuestionArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionArticleRepository extends JpaRepository<QuestionArticle, String> {

    // 특정 csatDate로 모든 QuestionArticle 가져오기
    List<QuestionArticle> findByCsatDate_CsatDate(String csatDate);

    // 특정 subclassId로 모든 QuestionArticle 가져오기
    List<QuestionArticle> findBySubclass_SubclassId(Long subclassId);

    // csatDate + subclassId로 모든 QuestionArticle 가져오기
    List<QuestionArticle> findByCsatDate_CsatDateAndSubclass_SubclassId(String csatDate, Long subclassId);

    // questionArticle 단건 조회
    Optional<QuestionArticle> findByQuestionArticle(String questionArticle);

}
