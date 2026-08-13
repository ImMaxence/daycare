package com.back.daycare.service;

import com.back.daycare.dto.response.DaycareDetailResponse;
import com.back.daycare.dto.response.MapDaycareResponse;
import com.back.daycare.entity.Daycare;
import com.back.daycare.entity.DaycareStatus;
import com.back.daycare.exception.ResourceNotFoundException;
import com.back.daycare.mapper.DaycareMapper;
import com.back.daycare.repository.DaycareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DaycareService {

    private final DaycareRepository daycareRepository;
    private final DaycareMapper daycareMapper;

    public List<MapDaycareResponse> getAllForMap() {
        return daycareRepository.findAllForMap();
    }

    public DaycareDetailResponse getDaycareDetails(UUID id) {
        return daycareRepository.findById(id)
                .map(daycareMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Crèche introuvable : " + id));
    }

    @Transactional
    public void updateStatus(UUID id, DaycareStatus newStatus) {
        Daycare daycare = daycareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crèche introuvable : " + id));

        daycare.setStatus(newStatus);
        daycareRepository.save(daycare);
    }
}

