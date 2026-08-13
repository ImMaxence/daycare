package com.back.daycare.repository;

import com.back.daycare.dto.response.MapDaycareResponse;
import com.back.daycare.entity.Daycare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DaycareRepository extends JpaRepository<Daycare, UUID> {

    @Query("SELECT new com.back.daycare.dto.response.MapDaycareResponse(d.id, d.latitude, d.longitude, d.status) FROM Daycare d")
    List<MapDaycareResponse> findAllForMap();
}