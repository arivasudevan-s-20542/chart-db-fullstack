package com.chartdb.repository;

import com.chartdb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // Find by email (for authentication) - case insensitive
    Optional<User> findByEmailIgnoreCase(String email);
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Check if email exists - case insensitive
    boolean existsByEmailIgnoreCase(String email);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Find active users
    List<User> findByIsActiveTrue();
    
    // Search users by email or username (for sharing)
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);
    
    // Update last login
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId, @Param("timestamp") Instant timestamp);
    
    // Find by multiple IDs
    List<User> findByIdIn(List<String> ids);
}
