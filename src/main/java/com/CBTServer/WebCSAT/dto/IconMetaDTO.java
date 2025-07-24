package com.CBTServer.WebCSAT.dto;

import com.CBTServer.WebCSAT.domain.IconMeta;
import com.CBTServer.WebCSAT.domain.ImageMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class IconMetaDTO {
    private String url;
    private String s3Key;

    public static IconMetaDTO from(IconMeta entity) {
        IconMetaDTO dto = new IconMetaDTO();
        dto.url = entity.getUrl();
        dto.s3Key = entity.getS3Key();
        return dto;
    }
}
