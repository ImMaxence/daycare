package com.back.daycare.dto.response;

import com.back.daycare.entity.DaycareStatus;
import com.back.daycare.entity.EstablishmentType;

import java.util.UUID;

public record MapDaycareResponse(
        UUID id,
        Double latitude,
        Double longitude,
        EstablishmentType type,
        DaycareStatus status
) {
}

