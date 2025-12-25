package com.chartdb.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipMessage {
    private String diagramId;
    private String relationshipId;
    private String action; // "created", "updated", "deleted"
    private String name;
    private String sourceTableId;
    private String sourceColumnId;
    private String targetTableId;
    private String targetColumnId;
    private String relationshipType; // ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    private String onDelete;
    private String onUpdate;
    private String userId;
    private String userName;
    private Long timestamp;
}
