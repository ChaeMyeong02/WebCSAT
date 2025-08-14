// src/main/java/com/CBTServer/WebCSAT/repository/GradeCutRepository.java
package com.CBTServer.WebCSAT.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradeCutRepository extends JpaRepository<com.CBTServer.WebCSAT.domain.SubclassCsatDate, Long> {

    interface Cuts {
        Integer getCut1(); Integer getCut2(); Integer getCut3();
        Integer getCut4(); Integer getCut5(); Integer getCut6();
        Integer getCut7(); Integer getCut8(); Integer getCut9();
    }

    @Query(value = """
      SELECT cut1, cut2, cut3, cut4, cut5, cut6, cut7, cut8, cut9
      FROM subclass_csat_date
      WHERE csat_date = :csatDate AND subclass_id = :subclassId
      LIMIT 1
      """, nativeQuery = true)
    Optional<Cuts> findCuts(@Param("csatDate") String csatDate,
                            @Param("subclassId") Long subclassId);
}
