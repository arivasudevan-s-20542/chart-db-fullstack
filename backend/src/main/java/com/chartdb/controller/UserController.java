package com.chartdb.controller;

import com.chartdb.dto.request.UpdateUserRequest;
import com.chartdb.dto.response.ApiResponse;
import com.chartdb.dto.response.UserResponse;
import com.chartdb.security.CurrentUser;
import com.chartdb.security.UserPrincipal;
import com.chartdb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.getUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Update current user's profile
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }
    
    /**
     * Search users (for sharing diagrams)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String query,
            @CurrentUser UserPrincipal currentUser) {
        List<UserResponse> users = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    /**
     * Get user by ID (for collaborator info)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable String userId,
            @CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
