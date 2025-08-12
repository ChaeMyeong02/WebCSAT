package com.CBTServer.WebCSAT.repository.proj;

import java.time.LocalDateTime;

public interface AttemptRow {
    Long getExamId();
    LocalDateTime getTestDate();
    String getCsatDate();
    String getSubclassName();
    Integer getScore();
}