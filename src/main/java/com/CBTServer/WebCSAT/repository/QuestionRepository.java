package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.Exam;
import com.CBTServer.WebCSAT.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository  extends JpaRepository<Question, Long> {
}
