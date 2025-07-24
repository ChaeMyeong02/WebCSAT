package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import com.CBTServer.WebCSAT.domain.ExamQuestion;
import com.CBTServer.WebCSAT.domain.Subclass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    // 특정 examId 기준으로 모든 ExamQuestion 조회
    List<ExamQuestion> findByExam_ExamId(Long examId);

    // 특정 subclassId 기준으로 모든 ExamQuestion 조회
    List<ExamQuestion> findBySubclass_SubclassId(Long subclassId);

    // 특정 examId + subclassId로 ExamQuestion 조회
    List<ExamQuestion> findByExam_ExamIdAndSubclass_SubclassId(Long examId, Long subclassId);

    // 단건 조회
    Optional<ExamQuestion> findByJoinId(Long joinId);

    // 삭제
    void deleteByJoinId(Long joinId);
}