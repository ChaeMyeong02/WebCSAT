package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Subject;
import com.CBTServer.WebCSAT.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectName(String subjectName);
}