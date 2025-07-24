package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubclassRepository extends JpaRepository<Subclass, Long> {
    List<Subclass> findAllBySubject(Subject subject);
    List<Subclass> findAllBySubject_SubjectId(Long subjectId);
    List<Subclass> findAllBySubject_SubjectName(String subjectName);

}