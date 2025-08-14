// com.CBTServer.WebCSAT.dto.GradeCutsResponse
package com.CBTServer.WebCSAT.dto;

public record GradeCutsResponse(
        String csatDate,
        Long subclassId,
        Integer cut1, Integer cut2, Integer cut3, Integer cut4, Integer cut5,
        Integer cut6, Integer cut7, Integer cut8, Integer cut9
) {}
