package com.chartdb.service;

import com.chartdb.dto.request.ExecuteQueryRequest;
import com.chartdb.dto.request.SaveQueryRequest;
import com.chartdb.dto.response.QueryExecutionResult;
import com.chartdb.dto.response.QueryHistoryResponse;
import com.chartdb.dto.response.SavedQueryResponse;
import com.chartdb.exception.ResourceNotFoundException;
import com.chartdb.model.*;
import com.chartdb.model.enums.QueryStatus;
import com.chartdb.repository.*;
import com.chartdb.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExecutionService {
    
    private final DatabaseConnectionRepository connectionRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final SavedQueryRepository savedQueryRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    
    @Transactional
    public QueryExecutionResult executeQuery(String connectionId, String userId, ExecuteQueryRequest request) {
        DatabaseConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String decryptedPassword = encryptionUtil.decrypt(connection.getEncryptedPassword());
        String jdbcUrl = buildJdbcUrl(connection);
        
        long startTime = System.currentTimeMillis();
        QueryExecutionResult result;
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connection.getUsername(), decryptedPassword);
             Statement stmt = conn.createStatement()) {
            
            boolean isResultSet = stmt.execute(request.getQuery());
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (isResultSet) {
                // SELECT query - return results
                try (ResultSet rs = stmt.getResultSet()) {
                    result = processResultSet(rs, executionTime);
                }
            } else {
                // UPDATE/INSERT/DELETE - return affected rows
                int rowsAffected = stmt.getUpdateCount();
                result = QueryExecutionResult.builder()
                    .success(true)
                    .message(rowsAffected + " row(s) affected")
                    .rowsAffected(rowsAffected)
                    .executionTimeMs(executionTime)
                    .build();
            }
            
            // Save to history
            saveToHistory(connection, user, request.getQuery(), executionTime, 
                result.getRowsAffected(), QueryStatus.SUCCESS, null);
            
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Query execution failed", e);
            
            result = QueryExecutionResult.builder()
                .success(false)
                .message("Query failed: " + e.getMessage())
                .errorCode(String.valueOf(e.getErrorCode()))
                .executionTimeMs(executionTime)
                .build();
            
            // Save error to history
            saveToHistory(connection, user, request.getQuery(), executionTime, 
                0, QueryStatus.ERROR, e.getMessage());
        }
        
        return result;
    }
    
    private QueryExecutionResult processResultSet(ResultSet rs, long executionTime) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Build column info
        List<Map<String, String>> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            Map<String, String> column = new HashMap<>();
            column.put("name", metaData.getColumnName(i));
            column.put("type", metaData.getColumnTypeName(i));
            columns.add(column);
        }
        
        // Build rows
        List<List<Object>> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < 1000) { // Limit to 1000 rows
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
            rowCount++;
        }
        
        return QueryExecutionResult.builder()
            .success(true)
            .message(rowCount + " row(s) returned")
            .columns(columns)
            .rows(rows)
            .rowsAffected(rowCount)
            .executionTimeMs(executionTime)
            .build();
    }
    
    private void saveToHistory(DatabaseConnection connection, User user, String query, 
                                long executionTime, int rowsAffected, QueryStatus status, String error) {
        QueryHistory history = QueryHistory.builder()
            .connection(connection)
            .user(user)
            .query(query)
            .executionTimeMs((int) executionTime)
            .rowsAffected(rowsAffected)
            .status(status)
            .errorMessage(error)
            .executedAt(Instant.now())
            .build();
        
        queryHistoryRepository.save(history);
    }
    
    @Transactional(readOnly = true)
    public Page<QueryHistoryResponse> getQueryHistory(String userId, Pageable pageable) {
        return queryHistoryRepository.findByUserIdOrderByExecutedAtDesc(userId, pageable)
            .map(this::mapHistoryToResponse);
    }
    
    @Transactional
    public SavedQueryResponse saveQuery(String userId, SaveQueryRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        SavedQuery savedQuery = SavedQuery.builder()
            .user(user)
            .name(request.getName())
            .description(request.getDescription())
            .query(request.getQuery())
            .tags(request.getTags())
            .isPublic(request.getIsPublic())
            .build();
        
        savedQuery = savedQueryRepository.save(savedQuery);
        return mapSavedQueryToResponse(savedQuery);
    }
    
    @Transactional(readOnly = true)
    public List<SavedQueryResponse> getSavedQueries(String userId) {
        return savedQueryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::mapSavedQueryToResponse)
            .collect(Collectors.toList());
    }
    
    private String buildJdbcUrl(DatabaseConnection connection) {
        String sslParam = Boolean.TRUE.equals(connection.getSslEnabled()) ? "&ssl=true" : "";
        
        return switch (connection.getDatabaseType().toLowerCase()) {
            case "postgresql" -> 
                String.format("jdbc:postgresql://%s:%d/%s?%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabaseName(), sslParam);
            case "mysql" -> 
                String.format("jdbc:mysql://%s:%d/%s?%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabaseName(), sslParam);
            case "sqlserver" -> 
                String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s", 
                    connection.getHost(), connection.getPort(), connection.getDatabaseName(), 
                    connection.getSslEnabled() ? "true" : "false");
            default -> throw new IllegalArgumentException("Unsupported database type: " + connection.getDatabaseType());
        };
    }
    
    private QueryHistoryResponse mapHistoryToResponse(QueryHistory history) {
        return QueryHistoryResponse.builder()
            .id(history.getId())
            .query(history.getQuery())
            .executionTimeMs(history.getExecutionTimeMs())
            .rowsAffected(history.getRowsAffected())
            .status(history.getStatus())
            .errorMessage(history.getErrorMessage())
            .executedAt(history.getExecutedAt())
            .build();
    }
    
    private SavedQueryResponse mapSavedQueryToResponse(SavedQuery query) {
        return SavedQueryResponse.builder()
            .id(query.getId())
            .name(query.getName())
            .description(query.getDescription())
            .query(query.getQuery())
            .tags(query.getTags())
            .isPublic(query.getIsPublic())
            .usageCount(query.getUsageCount())
            .createdAt(query.getCreatedAt())
            .build();
    }
}
