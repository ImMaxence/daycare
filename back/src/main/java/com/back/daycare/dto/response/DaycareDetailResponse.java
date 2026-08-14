package com.back.daycare.dto.response;

import com.back.daycare.entity.DaycareStatus;
import com.back.daycare.entity.EstablishmentType;

import java.util.UUID;

public record DaycareDetailResponse(
        UUID id,
        String externalId,
        EstablishmentType type,
        String name,
        Double latitude,
        Double longitude,
        String houseNumber,
        String street,
        String postcode,
        String city,
        String department,
        String phone,
        String email,
        String websiteUrl,
        String operator,
        String siret,
        String note,
        String source,
        DaycareStatus status
) {
}

