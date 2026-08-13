package com.back.daycare.mapper;

import com.back.daycare.dto.response.DaycareDetailResponse;
import com.back.daycare.entity.Daycare;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DaycareMapper extends EntityMapper<DaycareDetailResponse, Daycare> {
}


