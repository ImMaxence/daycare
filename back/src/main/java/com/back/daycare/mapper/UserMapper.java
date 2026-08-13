package com.back.daycare.mapper;

import com.back.daycare.dto.response.UserResponse;
import com.back.daycare.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper extends EntityMapper<UserResponse, User> {
}

