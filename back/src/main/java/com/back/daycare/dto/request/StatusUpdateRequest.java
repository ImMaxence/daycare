package com.back.daycare.dto.request;

import com.back.daycare.entity.DaycareStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Le statut est obligatoire")
        DaycareStatus status
) {
}

