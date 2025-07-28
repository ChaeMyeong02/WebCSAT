package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.domain.SubclassCsatDate;
import com.CBTServer.WebCSAT.dto.SubclassCsatDateDTO;
import com.CBTServer.WebCSAT.repository.SubclassCsatDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubclassCsatDateService {
    private final SubclassCsatDateRepository subclassCsatDateRepository;

    public List<SubclassCsatDateDTO> getAllSubclassCsatDates() {
        return subclassCsatDateRepository.findAll().stream()
                .filter(entity -> entity.getSubclass().isOptional()) // optional이 true인 것만 필터링
                .map(entity -> {
                    Subclass subclass = entity.getSubclass();
                    return SubclassCsatDateDTO.builder()
                            .subclassId(subclass.getSubclassId())
                            .csatDate(entity.getId().getCsatDate())
                            .listeningUrl(entity.getListeningUrl())
                            .cut1(entity.getCut1())
                            .cut2(entity.getCut2())
                            .cut3(entity.getCut3())
                            .cut4(entity.getCut4())
                            .cut5(entity.getCut5())
                            .cut6(entity.getCut6())
                            .cut7(entity.getCut7())
                            .cut8(entity.getCut8())
                            .cut9(entity.getCut9())
                            .subjectName(subclass.getSubject().getSubjectName())
                            .subclassName(subclass.getSubclassName())
                            .optional(subclass.isOptional())
                            .build();
                })
                .toList();
    }

    public List<SubclassCsatDateDTO> getListeningSubclassCsatDates() {
        return subclassCsatDateRepository.findBySubclass_ListeningTrue().stream()
                .map(entity -> {
                    Subclass subclass = entity.getSubclass();
                    return SubclassCsatDateDTO.builder()
                            .subclassId(subclass.getSubclassId())
                            .csatDate(entity.getId().getCsatDate())
                            .listeningUrl(entity.getListeningUrl())
                            .subjectName(subclass.getSubject().getSubjectName())
                            .subclassName(subclass.getSubclassName())
                            .optional(subclass.isOptional())
                            .build();
                })
                .toList();
    }
    @Transactional
    public void updateCuts(Long subclassId, String csatDate, List<Integer> cuts) {
        SubclassCsatDate entity = subclassCsatDateRepository.findById_SubclassIdAndId_CsatDate(subclassId, csatDate)
                .orElseThrow(() -> new NoSuchElementException("해당 시험 정보가 존재하지 않습니다."));

        if (cuts.size() != 9) {
            throw new IllegalArgumentException("cuts 리스트는 9개가 있어야 합니다.");
        }

        entity.setCut1(cuts.get(0));
        entity.setCut2(cuts.get(1));
        entity.setCut3(cuts.get(2));
        entity.setCut4(cuts.get(3));
        entity.setCut5(cuts.get(4));
        entity.setCut6(cuts.get(5));
        entity.setCut7(cuts.get(6));
        entity.setCut8(cuts.get(7));
        entity.setCut9(cuts.get(8));
    }

    public List<SubclassCsatDateDTO> getFilteredSubclassCsatDates(String subject, String csatDate) {
        return subclassCsatDateRepository.findAll().stream()
                .filter(entity -> entity.getSubclass().isOptional()) // optional true만
                .filter(entity -> {
                    if (subject != null && !subject.isBlank()) {
                        String subjectName = entity.getSubclass().getSubject().getSubjectName();
                        return subjectName != null && subjectName.contains(subject);
                    }
                    return true;
                })
                .filter(entity -> {
                    if (csatDate != null && !csatDate.isBlank()) {
                        String date = entity.getId().getCsatDate();
                        return date != null && date.contains(csatDate);
                    }
                    return true;
                })
                .map(entity -> {
                    Subclass subclass = entity.getSubclass();
                    return SubclassCsatDateDTO.builder()
                            .subclassId(subclass.getSubclassId())
                            .csatDate(entity.getId().getCsatDate())
                            .listeningUrl(entity.getListeningUrl())
                            .cut1(entity.getCut1())
                            .cut2(entity.getCut2())
                            .cut3(entity.getCut3())
                            .cut4(entity.getCut4())
                            .cut5(entity.getCut5())
                            .cut6(entity.getCut6())
                            .cut7(entity.getCut7())
                            .cut8(entity.getCut8())
                            .cut9(entity.getCut9())
                            .subjectName(subclass.getSubject().getSubjectName())
                            .subclassName(subclass.getSubclassName())
                            .optional(subclass.isOptional())
                            .build();
                })
                .toList();
    }

}
