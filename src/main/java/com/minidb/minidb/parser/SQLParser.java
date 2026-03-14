package com.minidb.minidb.parser;

import java.util.ArrayList;
import java.util.List;
import com.minidb.minidb.model.Query;

public class SQLParser {

    public Query parse(String sql) {
        Query q = new Query();
        String trimmed = sql.trim();

        if (trimmed.toUpperCase().startsWith("SHOW TABLES")) {
            q.type = "SHOW";
        }
        else if (trimmed.toUpperCase().startsWith("CREATE TABLE")) {
            q.type = "CREATE";
            String upper = trimmed.toUpperCase();
            int tableIdx = upper.indexOf("TABLE") + 5;
            int openIdx = trimmed.indexOf("(");
            q.tableName = trimmed.substring(tableIdx, openIdx).trim().toLowerCase();

            int close = trimmed.lastIndexOf(")");
            String inside = trimmed.substring(openIdx + 1, close);
            String[] colDefs = inside.split(",");
            List<String> columns = new ArrayList<>();
            for (String col : colDefs) {
                columns.add(col.trim().split("\\s+")[0].toLowerCase());
            }
            q.columns = columns;
        }
        else if (trimmed.toUpperCase().startsWith("DROP TABLE")) {
            q.type = "DROP";
            String upper = trimmed.toUpperCase();
            int tableIdx = upper.indexOf("TABLE") + 5;
            q.tableName = trimmed.substring(tableIdx).trim().toLowerCase();
        }
        else if (trimmed.toUpperCase().startsWith("ALTER TABLE")) {
            q.type = "ALTER";
            String upper = trimmed.toUpperCase();

            // ALTER TABLE <name> ADD <col> <type>
            // ALTER TABLE <name> DROP COLUMN <col>
            int tableIdx = upper.indexOf("TABLE") + 5;

            if (upper.contains("DROP COLUMN")) {
                int dropIdx = upper.indexOf("DROP COLUMN");
                q.tableName = trimmed.substring(tableIdx, dropIdx).trim().toLowerCase();
                q.alterAction = "DROP";
                q.alterColumn = trimmed.substring(dropIdx + 11).trim().toLowerCase();
            } else if (upper.contains("ADD")) {
                int addIdx = upper.indexOf("ADD");
                q.tableName = trimmed.substring(tableIdx, addIdx).trim().toLowerCase();
                q.alterAction = "ADD";
                String rest = trimmed.substring(addIdx + 3).trim();
                String[] parts = rest.split("\\s+");
                q.alterColumn = parts[0].toLowerCase();
                q.alterType = parts.length > 1 ? parts[1].toUpperCase() : "TEXT";
            }
        }
        else if (trimmed.toUpperCase().startsWith("SELECT")) {
            q.type = "SELECT";
            String upper = trimmed.toUpperCase();

            String[] fromParts = trimmed.split("(?i)\\bFROM\\b");
            String afterFrom = fromParts[1].trim();

            // Check ORDER BY first
            if (upper.contains("ORDER BY")) {
                String[] orderParts = afterFrom.split("(?i)\\bORDER BY\\b");
                q.orderByColumn = orderParts[1].trim().toLowerCase();
                afterFrom = orderParts[0].trim();
            }

            if (upper.contains("WHERE")) {
                String[] whereParts = afterFrom.split("(?i)\\bWHERE\\b");
                q.tableName = whereParts[0].trim().toLowerCase();
                parseWhere(whereParts[1].trim(), q);
            } else {
                q.tableName = afterFrom.split("\\s+")[0].toLowerCase();
            }
        }
        else if (trimmed.toUpperCase().startsWith("INSERT")) {
            q.type = "INSERT";
            String upper = trimmed.toUpperCase();
            int intoIdx = upper.indexOf("INTO") + 4;
            int valuesIdx = upper.indexOf("VALUES");
            q.tableName = trimmed.substring(intoIdx, valuesIdx).trim().toLowerCase();

            int open = trimmed.indexOf("(");
            int close = trimmed.lastIndexOf(")");
            if (open != -1 && close != -1) {
                String inside = trimmed.substring(open + 1, close);
                String[] rawValues = inside.split(",");
                List<String> values = new ArrayList<>();
                for (String v : rawValues) {
                    values.add(v.trim());
                }
                q.values = values;
            }
        }
        else if (trimmed.toUpperCase().startsWith("DELETE")) {
            q.type = "DELETE";
            String upper = trimmed.toUpperCase();

            String[] fromParts = trimmed.split("(?i)\\bFROM\\b");
            String afterFrom = fromParts[1].trim();

            if (upper.contains("WHERE")) {
                String[] whereParts = afterFrom.split("(?i)\\bWHERE\\b");
                q.tableName = whereParts[0].trim().toLowerCase();
                parseWhere(whereParts[1].trim(), q);
            } else {
                q.tableName = afterFrom.split("\\s+")[0].toLowerCase();
            }
        }
        else if (trimmed.toUpperCase().startsWith("UPDATE")) {
            q.type = "UPDATE";
            String upper = trimmed.toUpperCase();

            int setIdx = upper.indexOf("SET");
            q.tableName = trimmed.substring(6, setIdx).trim().toLowerCase();

            int whereIdx = upper.indexOf("WHERE");
            String setPart;
            if (whereIdx != -1) {
                setPart = trimmed.substring(setIdx + 3, whereIdx).trim();
                parseWhere(trimmed.substring(whereIdx + 5).trim(), q);
            } else {
                setPart = trimmed.substring(setIdx + 3).trim();
            }

            String[] setParts = setPart.split("=", 2);
            q.setColumn = setParts[0].trim().toLowerCase();
            q.setValue = setParts[1].trim();
        }
        else {
            q.type = "UNKNOWN";
        }

        return q;
    }

    private void parseWhere(String whereClause, Query q) {
        String[] conditions = whereClause.split("(?i)\\s+AND\\s+");
        for (String condition : conditions) {
            String[] parts = condition.split("=", 2);
            q.whereColumns.add(parts[0].trim().toLowerCase());
            q.whereValues.add(parts[1].trim());
        }
    }
}
