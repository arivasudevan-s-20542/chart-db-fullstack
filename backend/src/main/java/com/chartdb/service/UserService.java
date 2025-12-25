package com.chartdb.service;

import com.chartdb.dto.request.UpdateUserRequest;
import com.chartdb.dto.response.UserResponse;
import com.chartdb.model.User;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.mapper.UserMapper;
import com.chartdb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }
    
    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Updated user profile: {}", userId);
        
        return userMapper.toResponse(savedUser);
    }
    
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        List<User> users = userRepository.searchUsers(query);
        return users.stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
