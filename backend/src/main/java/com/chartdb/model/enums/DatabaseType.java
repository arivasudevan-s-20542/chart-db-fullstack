package com.chartdb.model.enums;

public enum DatabaseType {
    POSTGRESQL("postgresql", 5432),
    MYSQL("mysql", 3306),
    SQLSERVER("sqlserver", 1433),
    ORACLE("oracle", 1521),
    MONGODB("mongodb", 27017);

    private final String type;
    private final int defaultPort;

    DatabaseType(String type, int defaultPort) {
        this.type = type;
        this.defaultPort = defaultPort;
    }

    public String getType() {
        return type;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public static DatabaseType fromString(String type) {
        for (DatabaseType dbType : DatabaseType.values()) {
            if (dbType.type.equalsIgnoreCase(type)) {
                return dbType;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + type);
    }
}
