package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.CsatDate;
import com.CBTServer.WebCSAT.domain.Question;
import com.CBTServer.WebCSAT.domain.Subclass;
import com.CBTServer.WebCSAT.domain.Subject;
import com.CBTServer.WebCSAT.dto.CsatDateDTO;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.SubclassDTO;
import com.CBTServer.WebCSAT.dto.SubjectDTO;
import com.CBTServer.WebCSAT.repository.SubclassRepository;
import com.CBTServer.WebCSAT.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SubjectSubclassService {
    private final SubjectRepository subjectRepository;
    private final SubclassRepository subclassRepository;

    public List<SubjectDTO> findAllSubject() {
        List<Subject> subjectList = subjectRepository.findAll();
        List<SubjectDTO> dtoList = new ArrayList<>();
        for(Subject s : subjectList) {
            SubjectDTO dto = new SubjectDTO(s.getSubjectId(), s.getSubjectName(), s.getOption(), s.getMin());
            dtoList.add(dto);
        }
        return dtoList;
    }

    public List<SubclassDTO> findAllSubclass() {
        List<Subclass> subclassList = subclassRepository.findAll();
        List<SubclassDTO> dtoList = new ArrayList<>();
        for (Subclass s : subclassList) {
            SubclassDTO dto = SubclassDTO.builder()
                    .subclassId(s.getSubclassId())
                    .subclassName(s.getSubclassName())
                    .count(s.getCount())
                    .optional(s.isOptional())
                    .subjectId(s.getSubject() != null ? s.getSubject().getSubjectId() : null)
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

    public SubjectDTO saveSubject(SubjectDTO subjectDTO) {
        Subject subject = new Subject();
        subject.setSubjectName(subjectDTO.getSubjectName());
        subject.setMin(subjectDTO.getMin());
        subject.setOption(subjectDTO.getOption());
        Subject saved = subjectRepository.save(subject);
        return new SubjectDTO(saved.getSubjectId(), saved.getSubjectName(), saved.getOption(), saved.getMin());
    }

    public SubclassDTO saveSubclass(SubclassDTO subclassDTO) {
        Subject subject = subjectRepository.findById(subclassDTO.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException(subclassDTO.getSubjectId() + " : 존재하지 않는 Subject ID 입니다."));

        Subclass subclass = new Subclass();
        subclass.setSubclassName(subclassDTO.getSubclassName());
        subclass.setSubject(subject);
        subclass.setOptional(subclassDTO.isOptional());
        subclass.setCount(subclassDTO.getCount());

        Subclass saved = subclassRepository.save(subclass);

        return SubclassDTO.builder()
                .subclassId(saved.getSubclassId())
                .subclassName(saved.getSubclassName())
                .count(saved.getCount())
                .optional(saved.isOptional())
                .subjectId(saved.getSubject() != null ? saved.getSubject().getSubjectId() : null)
                .build();
    }

    public SubjectDTO findBySubjectId(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new IllegalArgumentException(subjectId + " : 존재하지 않는 ID 입니다."));
        return new SubjectDTO(subject.getSubjectId(), subject.getSubjectName(), subject.getOption(), subject.getMin());
    }

    public SubclassDTO findBySubclassId(Long subclassId) {
        Subclass subclass = subclassRepository.findById(subclassId).orElseThrow(() -> new IllegalArgumentException(subclassId + " : 존재하지 않는 ID 입니다."));
        return SubclassDTO.builder()
                .subclassId(subclass.getSubclassId())
                .subclassName(subclass.getSubclassName())
                .count(subclass.getCount())
                .optional(subclass.isOptional())
                .subjectId(subclass.getSubject() != null ? subclass.getSubject().getSubjectId() : null)
                .build();
    }

    public SubjectDTO updateSubject(Long subjectId, SubjectDTO subjectDTO) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("과목이 존재하지 않습니다."));

        subject.setSubjectName(subjectDTO.getSubjectName());
        subject.setMin(subjectDTO.getMin());
        subject.setOption(subjectDTO.getOption());

        Subject updated = subjectRepository.save(subject);
        return new SubjectDTO(updated.getSubjectId(), updated.getSubjectName(), updated.getOption(), updated.getMin());
    }

    public SubclassDTO updateSubclass(Long subclassId, SubclassDTO subclassDTO) {
        Subclass subclass = subclassRepository.findById(subclassId)
                .orElseThrow(() -> new IllegalArgumentException("선택과목이 존재하지 않습니다."));
        subclass.setSubclassName(subclassDTO.getSubclassName());
        subclass.setCount(subclassDTO.getCount());
        subclass.setOptional(subclassDTO.isOptional());

        if (subclassDTO.getSubjectId() != null) {
            Subject newSubject = subjectRepository.findById(subclassDTO.getSubjectId())
                    .orElseThrow(() -> new IllegalArgumentException("지정한 과목이 존재하지 않습니다."));
            subclass.setSubject(newSubject);
        }

        Subclass updated = subclassRepository.save(subclass);

        return SubclassDTO.builder()
                .subclassId(updated.getSubclassId())
                .subclassName(updated.getSubclassName())
                .count(updated.getCount())
                .optional(updated.isOptional())
                .subjectId(updated.getSubject() != null ? updated.getSubject().getSubjectId() : null)
                .build();
    }


    public void deleteSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("과목이 존재하지 않습니다."));
        subjectRepository.delete(subject);
    }

    public void deleteSubclass(Long subclassId) {
        Subclass subclass = subclassRepository.findById(subclassId)
                .orElseThrow(() -> new IllegalArgumentException("선택과목이 존재하지 않습니다."));

        subclassRepository.delete(subclass);

    }
}
