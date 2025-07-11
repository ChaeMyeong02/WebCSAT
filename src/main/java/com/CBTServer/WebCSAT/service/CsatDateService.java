package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.dto.CsatDateDTO;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CsatDateService {
    private final CsatDateRepository csatDateRepository;

    public CsatDateDTO saveIfNotExist(String dateStr) {
        if (csatDateRepository.existsById(dateStr)) {
            throw new IllegalArgumentException(dateStr + " : 이미 존재하는 날짜입니다.");
        }
        CsatDate csatDate = new CsatDate();
        csatDate.setCsatDate(dateStr);
        CsatDate saved = csatDateRepository.save(csatDate);
        return new CsatDateDTO(saved.getCsatDate());
    }
}
