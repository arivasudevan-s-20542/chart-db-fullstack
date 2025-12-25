package com.chartdb.mapper;

import com.chartdb.dto.response.UserResponse;
import com.chartdb.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    
    UserResponse toResponse(User user);
    
    List<UserResponse> toResponseList(List<User> users);
}
