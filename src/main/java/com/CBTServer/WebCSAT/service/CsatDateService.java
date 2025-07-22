package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.dto.CsatDateDTO;
import com.CBTServer.WebCSAT.repository.CsatDateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CsatDateService {
    private final CsatDateRepository csatDateRepository;

    @Transactional
    public CsatDateDTO saveIfNotExist(String dateStr) {
        if (csatDateRepository.existsById(dateStr)) {
            throw new IllegalArgumentException(dateStr + " : 이미 존재하는 날짜입니다.");
        }
        CsatDate csatDate = new CsatDate();
        csatDate.setCsatDate(dateStr);
        CsatDate saved = csatDateRepository.save(csatDate);
        return new CsatDateDTO(saved.getCsatDate());
    }

    public List<CsatDate> findAllDate() {
        return csatDateRepository.findAll();
    }

    public Optional<CsatDate> findByDate(String str) {
        return csatDateRepository.findById(str);
    }
}
