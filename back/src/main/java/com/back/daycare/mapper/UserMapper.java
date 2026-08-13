package com.back.daycare.mapper;

import com.back.daycare.dto.response.UserResponse;
import com.back.daycare.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}

