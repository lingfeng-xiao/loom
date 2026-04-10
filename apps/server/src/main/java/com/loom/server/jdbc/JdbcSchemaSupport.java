package com.loom.server.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcSchemaSupport {
    private JdbcSchemaSupport() {
    }

    public static void ensureIndex(JdbcTemplate jdbcTemplate, String tableName, String indexName, String columnsSql) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """,
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + "(" + columnsSql + ")");
        }
    }

    public static void ensureColumn(JdbcTemplate jdbcTemplate, String tableName, String columnName, String columnDefinition) {
        if (!hasColumn(jdbcTemplate, tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    public static void renameColumnIfExists(JdbcTemplate jdbcTemplate, String tableName, String oldName, String newName) {
        if (hasColumn(jdbcTemplate, tableName, oldName) && !hasColumn(jdbcTemplate, tableName, newName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " RENAME COLUMN " + oldName + " TO " + newName);
        }
    }

    private static boolean hasColumn(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}

