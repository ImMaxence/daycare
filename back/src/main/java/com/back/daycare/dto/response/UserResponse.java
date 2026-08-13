package com.back.daycare.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        LocalDateTime lastConnexion
) {
}


