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
}
