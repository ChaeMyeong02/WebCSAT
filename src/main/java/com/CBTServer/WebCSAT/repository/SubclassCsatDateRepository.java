package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.SubclassCsatDate;
import com.CBTServer.WebCSAT.domain.SubclassCsatDateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubclassCsatDateRepository extends JpaRepository<SubclassCsatDate, SubclassCsatDateId> {
    Optional<SubclassCsatDate> findById_SubclassIdAndId_CsatDate(Long subclassId, String csatDate);
    List<SubclassCsatDate> findBySubclass_ListeningTrue();
}
