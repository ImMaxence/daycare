package com.back.daycare.mapper;

import com.back.daycare.dto.response.UserResponse;
import com.back.daycare.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper extends EntityMapper<UserResponse, User> {
}

