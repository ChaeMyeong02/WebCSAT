package com.CBTServer.WebCSAT.dto;

import java.time.LocalDateTime;

public record AttemptTrendDTO(
        Long examId,
        Long subclassId,
        String csatDate,
        LocalDateTime testDate,
        Double result

) {}