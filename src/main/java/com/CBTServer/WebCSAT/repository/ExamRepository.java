package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import com.CBTServer.WebCSAT.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    // (A) examId만으로 찾기
    @Query("select e from Exam e join fetch e.subclass sc join fetch e.user u " +
            "where e.examId = :examId")
    Optional<Exam> findByExamId(@Param("examId") Long examId);

    // (B) 소유자 검증까지 함께 (user.userId 필드명 사용!)
    @Query("select e from Exam e join fetch e.subclass sc join fetch e.user u " +
            "where e.examId = :examId and u.userId = :userId")
    Optional<Exam> findOwnedExam(@Param("examId") Long examId,
                                 @Param("userId") Long userId);


    Optional<Exam> findByExamIdAndUser_UserId(Long examId, Long userId);


    // 시험 기록 삭제
    void deleteByExamId(Long examId);

    List<Exam> findByUser(User user);

    List<Exam> findByUserAndCsatDate_CsatDate(User user, String csatDate);

    int countByUser(User user);
}
