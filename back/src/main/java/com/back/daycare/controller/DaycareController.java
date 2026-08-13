package com.back.daycare.controller;

import com.back.daycare.dto.request.StatusUpdateRequest;
import com.back.daycare.dto.response.DaycareDetailResponse;
import com.back.daycare.dto.response.MapDaycareResponse;
import com.back.daycare.service.DaycareService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/daycares")
@RequiredArgsConstructor
public class DaycareController {

    private final DaycareService daycareService;

    @Operation(operationId = "getDaycaresForMap", tags = {"daycare"})
    @GetMapping(value = "/map", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MapDaycareResponse>> getAllForMap() {
        return ResponseEntity.ok(daycareService.getAllForMap());
    }

    @Operation(operationId = "getDaycareById", tags = {"daycare"})
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DaycareDetailResponse> getDaycareDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(daycareService.getDaycareDetails(id));
    }

    @Operation(operationId = "updateDaycareStatus", tags = {"daycare"})
    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        daycareService.updateStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }
}

