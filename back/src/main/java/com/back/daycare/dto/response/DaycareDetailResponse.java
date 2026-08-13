package com.back.daycare.dto.response;

import com.back.daycare.entity.DaycareStatus;

import java.util.UUID;

public record DaycareDetailResponse(
        UUID id,
        Long osmId,
        String name,
        Double latitude,
        Double longitude,
        String houseNumber,
        String street,
        String postcode,
        String city,
        String phone,
        String operator,
        String siret,
        String note,
        String source,
        DaycareStatus status
) {
}

