package com.back.daycare.dto.response;

import com.back.daycare.entity.DaycareStatus;

import java.util.UUID;

public record MapDaycareResponse(
        UUID id,
        Double latitude,
        Double longitude,
        DaycareStatus status
) {
}

