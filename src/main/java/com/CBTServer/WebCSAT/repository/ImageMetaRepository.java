package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.ImageMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageMetaRepository extends JpaRepository<ImageMeta, String> {
    List<ImageMeta> findByCsatDate_CsatDateAndSubclass_SubclassId(String csatDate, Long subclassId);

    List<ImageMeta> findByCsatDate_CsatDate(String csatDate);

    List<ImageMeta> findBySubclass_SubclassId(Long subclassId);
}