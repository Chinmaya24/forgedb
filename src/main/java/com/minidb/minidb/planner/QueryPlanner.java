package com.minidb.minidb.planner;

import com.minidb.minidb.model.Query;
import java.util.List;
import java.util.Map;

public class QueryPlanner {

    private final Map<String, List<String>> schemas;
    private final Map<String, List<List<String>>> tables;

    public QueryPlanner(Map<String, List<String>> schemas,
                        Map<String, List<List<String>>> tables) {
        this.schemas = schemas;
        this.tables  = tables;
    }

    public QueryPlan plan(Query q) {
        switch (q.type.toUpperCase()) {

            case "SHOW":
                return QueryPlan.valid("SHOW", null,
                    QueryPlan.ScanType.NO_SCAN, 0,
                    "List all tables");

            case "CREATE":
                if (schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' already exists.");
                }
                return QueryPlan.valid("CREATE", q.tableName,
                    QueryPlan.ScanType.NO_SCAN, 0,
                    "Create new table '" + q.tableName + "' with columns " + q.columns);

            case "DROP":
                if (!schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' does not exist.");
                }
                return QueryPlan.valid("DROP", q.tableName,
                    QueryPlan.ScanType.NO_SCAN, 0,
                    "Drop table '" + q.tableName + "' and all its data");

            case "ALTER":
                if (!schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' does not exist.");
                }
                List<String> alterCols = schemas.get(q.tableName);
                if (q.alterAction.equalsIgnoreCase("ADD") && alterCols.contains(q.alterColumn)) {
                    return QueryPlan.invalid("Column '" + q.alterColumn + "' already exists.");
                }
                if (q.alterAction.equalsIgnoreCase("DROP") && !alterCols.contains(q.alterColumn)) {
                    return QueryPlan.invalid("Column '" + q.alterColumn + "' does not exist.");
                }
                return QueryPlan.valid("ALTER", q.tableName,
                    QueryPlan.ScanType.NO_SCAN, 0,
                    q.alterAction + " column '" + q.alterColumn + "' on table '" + q.tableName + "'");

            case "INSERT":
                if (!schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' does not exist.");
                }
                List<String> insertCols = schemas.get(q.tableName);
                if (q.values.size() != insertCols.size()) {
                    return QueryPlan.invalid("Column count mismatch: expected " +
                        insertCols.size() + " values but got " + q.values.size());
                }
                return QueryPlan.valid("INSERT", q.tableName,
                    QueryPlan.ScanType.NO_SCAN, 1,
                    "Insert 1 row into '" + q.tableName + "'");

            case "SELECT": {
                if (!schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' does not exist.");
                }
                List<String> cols = schemas.get(q.tableName);
                for (String wc : q.whereColumns) {
                    if (!cols.contains(wc)) {
                        return QueryPlan.invalid("Column '" + wc + "' does not exist.");
                    }
                }
                if (q.orderByColumn != null && !cols.contains(q.orderByColumn)) {
                    return QueryPlan.invalid("Column '" + q.orderByColumn + "' does not exist.");
                }
                int rowCount = tables.get(q.tableName).size();
                String desc = q.whereColumns.isEmpty()
                    ? "Full scan of '" + q.tableName + "' (" + rowCount + " rows)"
                    : "Full scan of '" + q.tableName + "' with filter on " + q.whereColumns;
                return QueryPlan.valid("SELECT", q.tableName,
                    QueryPlan.ScanType.FULL_TABLE_SCAN, rowCount, desc);
            }

            case "UPDATE": {
                if (!schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' does not exist.");
                }
                List<String> cols = schemas.get(q.tableName);
                if (!cols.contains(q.setColumn)) {
                    return QueryPlan.invalid("Column '" + q.setColumn + "' does not exist.");
                }
                for (String wc : q.whereColumns) {
                    if (!cols.contains(wc)) {
                        return QueryPlan.invalid("Column '" + wc + "' does not exist.");
                    }
                }
                int rowCount = tables.get(q.tableName).size();
                return QueryPlan.valid("UPDATE", q.tableName,
                    QueryPlan.ScanType.FULL_TABLE_SCAN, rowCount,
                    "Scan '" + q.tableName + "' and update column '" + q.setColumn + "'");
            }

            case "DELETE": {
                if (!schemas.containsKey(q.tableName)) {
                    return QueryPlan.invalid("Table '" + q.tableName + "' does not exist.");
                }
                List<String> cols = schemas.get(q.tableName);
                for (String wc : q.whereColumns) {
                    if (!cols.contains(wc)) {
                        return QueryPlan.invalid("Column '" + wc + "' does not exist.");
                    }
                }
                int rowCount = tables.get(q.tableName).size();
                return QueryPlan.valid("DELETE", q.tableName,
                    QueryPlan.ScanType.FULL_TABLE_SCAN, rowCount,
                    "Scan '" + q.tableName + "' and delete matching rows");
            }

            default:
                return QueryPlan.invalid("Unsupported query type: " + q.type);
        }
    }
}
