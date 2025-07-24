package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    // 특정 사용자(userId) 기준으로 시험 기록 조회
    List<Exam> findByUser_UserId(Long userId);

    // 특정 subclassId 기준으로 시험 기록 조회
    List<Exam> findBySubclass_SubclassId(Long subclassId);

    // 특정 csatDate 기준으로 시험 기록 조회
    List<Exam> findByCsatDate_CsatDate(String csatDate);

    // 사용자 + csatDate로 시험 기록 조회
    List<Exam> findByUser_UserIdAndCsatDate_CsatDate(Long userId, String csatDate);

    // 사용자 + subclassId로 시험 기록 조회
    List<Exam> findByUser_UserIdAndSubclass_SubclassId(Long userId, Long subclassId);

    // 단건 조회
    Optional<Exam> findByExamId(Long examId);

    // 시험 기록 삭제
    void deleteByExamId(Long examId);
}