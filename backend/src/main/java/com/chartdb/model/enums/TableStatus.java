package com.chartdb.model.enums;

public enum TableStatus {
    PLANNED,      // Only exists in diagram (not in actual database)
    EXISTS,       // Exists in connected database
    SYNCED,       // Diagram and database are in sync
    DRIFT,        // Exists but diagram differs from database
    DEPRECATED    // Marked for removal (future feature)
}
