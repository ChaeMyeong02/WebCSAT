package com.CBTServer.WebCSAT.repository;

import com.CBTServer.WebCSAT.domain.IconMeta;
import com.CBTServer.WebCSAT.domain.ImageMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IconMetaRepository extends JpaRepository<IconMeta, String> {
}