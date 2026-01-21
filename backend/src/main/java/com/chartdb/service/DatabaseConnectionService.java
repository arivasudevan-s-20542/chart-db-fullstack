package com.chartdb.service;

import com.chartdb.dto.request.CreateConnectionRequest;
import com.chartdb.dto.request.TestConnectionRequest;
import com.chartdb.dto.response.ConnectionResponse;
import com.chartdb.dto.response.ConnectionTestResult;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.model.DatabaseConnection;
import com.chartdb.model.Diagram;
import com.chartdb.model.User;
import com.chartdb.model.enums.ConnectionStatus;
import com.chartdb.repository.DatabaseConnectionRepository;
import com.chartdb.repository.DiagramRepository;
import com.chartdb.repository.UserRepository;
import com.chartdb.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*; 
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {
    
    private final DatabaseConnectionRepository connectionRepository;
    private final DiagramRepository diagramRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    
    @Transactional
    public ConnectionResponse createConnection(String userId, CreateConnectionRequest request) {
        // Validate diagram exists and user has access
        Diagram diagram = diagramRepository.findById(request.getDiagramId())
            .orElseThrow(() -> new ResourceNotFoundException("Diagram not found"));
        
        // Check if user owns the diagram or has edit permission
        if (!diagram.getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException("You don't have permission to add connections to this diagram");
        }
        
        // Check for duplicate name
        if (connectionRepository.existsByDiagramIdAndName(request.getDiagramId(), request.getName())) {
            throw new IllegalArgumentException("Connection with this name already exists");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Encrypt password
        String encryptedPassword = encryptionUtil.encrypt(request.getPassword());
        
        DatabaseConnection connection = DatabaseConnection.builder()
            .diagram(diagram)
            .user(user)
            .name(request.getName())
            .databaseType(request.getDatabaseType())
            .host(request.getHost())
            .port(request.getPort())
            .databaseName(request.getDatabaseName())
            .username(request.getUsername())
            .encryptedPassword(encryptedPassword)
            .sslEnabled(request.getSslEnabled())
            .additionalParams(request.getAdditionalParams())
            .status(ConnectionStatus.UNKNOWN)
            .build();
        
        connection = connectionRepository.save(connection);
        log.info("Created database connection: {} for diagram: {}", connection.getId(), diagram.getId());
        
        return mapToResponse(connection);
    }
    
    @Transactional(readOnly = true)
    public List<ConnectionResponse> getConnectionsByDiagram(String diagramId, String userId) {
        // Validate diagram exists
        diagramRepository.findById(diagramId)
            .orElseThrow(() -> new ResourceNotFoundException("Diagram not found"));
        
        return connectionRepository.findByDiagramIdOrderByCreatedAtDesc(diagramId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ConnectionResponse getConnection(String connectionId, String userId) {
        DatabaseConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        
        return mapToResponse(connection);
    }
    
    @Transactional
    public void deleteConnection(String connectionId, String userId) {
        DatabaseConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        
        connectionRepository.delete(connection);
        log.info("Deleted database connection: {}", connectionId);
    }
    
    public ConnectionTestResult testConnection(TestConnectionRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String jdbcUrl = buildJdbcUrl(request.getDatabaseType(), request.getHost(), 
                request.getPort(), request.getDatabaseName(), false);// as of now hardcoded
            
            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl, 
                    request.getUsername(), 
                    request.getPassword())) {
                
                DatabaseMetaData metaData = conn.getMetaData();
                long duration = System.currentTimeMillis() - startTime;
                
                return ConnectionTestResult.builder()
                    .success(true)
                    .message("Connection successful")
                    .databaseProduct(metaData.getDatabaseProductName())
                    .databaseVersion(metaData.getDatabaseProductVersion())
                    .responseTimeMs(duration)
                    .build();
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Connection test failed", e);
            
            return ConnectionTestResult.builder()
                .success(false)
                .message("Connection failed: " + e.getMessage())
                .errorCode(String.valueOf(e.getErrorCode()))
                .responseTimeMs(duration)
                .build();
        }
    }
    
    @Transactional
    public ConnectionTestResult testExistingConnection(String connectionId, String userId) {
        DatabaseConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        
        String decryptedPassword = encryptionUtil.decrypt(connection.getEncryptedPassword());
        
        TestConnectionRequest request = TestConnectionRequest.builder()
            .databaseType(connection.getDatabaseType())
            .host(connection.getHost())
            .port(connection.getPort())
            .databaseName(connection.getDatabaseName())
            .username(connection.getUsername())
            .password(decryptedPassword)
            .sslEnabled(connection.getSslEnabled())
            .build();
        
        ConnectionTestResult result = testConnection(request);
        
        // Update connection status
        if (result.isSuccess()) {
            connection.setStatus(ConnectionStatus.CONNECTED);
            connection.setLastConnectedAt(Instant.now());
            connection.setLastError(null);
        } else {
            connection.setStatus(ConnectionStatus.ERROR);
            connection.setLastError(result.getMessage());
        }
        
        connectionRepository.save(connection);
        
        return result;
    }
    
    private String buildJdbcUrl(String databaseType, String host, Integer port, 
                                 String databaseName, Boolean sslEnabled) {
        boolean useSsl = Boolean.TRUE.equals(sslEnabled);
        
        return switch (databaseType.toLowerCase()) {
            case "postgresql" -> {
                String sslParam = useSsl ? "ssl=true&sslmode=require" : "sslmode=disable";
                yield String.format("jdbc:postgresql://%s:%d/%s?%s", host, port, databaseName, sslParam);
            }
            case "mysql" -> {
                String sslParam = useSsl ? "useSSL=true&requireSSL=true" : "useSSL=false";
                yield String.format("jdbc:mysql://%s:%d/%s?%s", host, port, databaseName, sslParam);
            }
            case "sqlserver" -> 
                String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s", 
                    host, port, databaseName, useSsl ? "true" : "false");
            case "oracle" -> 
                String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
    }
    
    private ConnectionResponse mapToResponse(DatabaseConnection connection) {
        return ConnectionResponse.builder()
            .id(connection.getId())
            .diagramId(connection.getDiagram().getId())
            .name(connection.getName())
            .databaseType(connection.getDatabaseType())
            .host(connection.getHost())
            .port(connection.getPort())
            .databaseName(connection.getDatabaseName())
            .username(connection.getUsername())
            .sslEnabled(connection.getSslEnabled())
            .status(connection.getStatus())
            .lastConnectedAt(connection.getLastConnectedAt())
            .lastError(connection.getLastError())
            .createdAt(connection.getCreatedAt())
            .updatedAt(connection.getUpdatedAt())
            .build();
    }
}
