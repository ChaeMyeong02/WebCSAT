// com.CBTServer.WebCSAT.controller.GradeCutLookupController
package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.dto.GradeCutsResponse;
import com.CBTServer.WebCSAT.repository.SubclassCsatDateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lookup")
public class GradeCutLookupController {

    private final SubclassCsatDateRepository repo;

    public GradeCutLookupController(SubclassCsatDateRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/grade-cuts")
    public ResponseEntity<?> getGradeCuts(
            @RequestParam String csatDate,
            @RequestParam Long subclassId
    ) {
        return repo.findById_SubclassIdAndId_CsatDate(subclassId, csatDate)
                .<ResponseEntity<?>>map(scd -> {
                    // ✅ 키값은 id에서, 또는 연관엔티티에서 안전하게 꺼내기
                    String cd = scd.getId() != null ? scd.getId().getCsatDate()
                            : (scd.getCsatDate() != null ? scd.getCsatDate().getCsatDate() : null);
                    Long sc = scd.getId() != null ? scd.getId().getSubclassId()
                            : (scd.getSubclass() != null ? scd.getSubclass().getSubclassId() : null);

                    return ResponseEntity.ok(
                            new GradeCutsResponse(
                                    cd, sc,
                                    scd.getCut1(), scd.getCut2(), scd.getCut3(), scd.getCut4(), scd.getCut5(),
                                    scd.getCut6(), scd.getCut7(), scd.getCut8(), scd.getCut9()
                            )
                    );
                })
                .orElseGet(() -> ResponseEntity.noContent().build()); // 204: 등급컷 없음
    }
}
