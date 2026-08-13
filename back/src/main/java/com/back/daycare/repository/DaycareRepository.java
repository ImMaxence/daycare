package com.back.daycare.repository;

import com.back.daycare.entity.Daycare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DaycareRepository extends JpaRepository<Daycare, UUID> {
}


