package com.back.daycare.repository;

import com.back.daycare.dto.response.MapDaycareResponse;
import com.back.daycare.entity.Daycare;
import com.back.daycare.entity.DaycareStatus;
import com.back.daycare.entity.EstablishmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DaycareRepository extends JpaRepository<Daycare, UUID> {

    @Query("SELECT new com.back.daycare.dto.response.MapDaycareResponse(d.id, d.latitude, d.longitude, d.type, d.status) FROM Daycare d")
    List<MapDaycareResponse> findAllForMap();

    @Query("""
            SELECT d FROM Daycare d
            WHERE (:type IS NULL OR d.type = :type)
            AND (:status IS NULL OR d.status = :status)
            AND (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    List<Daycare> search(@Param("type") EstablishmentType type,
                          @Param("status") DaycareStatus status,
                          @Param("name") String name);
}