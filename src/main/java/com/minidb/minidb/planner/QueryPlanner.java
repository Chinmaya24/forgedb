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
                if ((q.insertColumns == null || q.insertColumns.isEmpty()) && q.values.size() != insertCols.size()) {
                    return QueryPlan.invalid("Column count mismatch: expected " +
                        insertCols.size() + " values but got " + q.values.size());
                }
                if (q.insertColumns != null && !q.insertColumns.isEmpty()) {
                    if (q.insertColumns.size() != q.values.size()) {
                        return QueryPlan.invalid("Column list and values count mismatch.");
                    }
                    for (String col : q.insertColumns) {
                        if (!insertCols.contains(col)) {
                            return QueryPlan.invalid("Column '" + col + "' does not exist.");
                        }
                    }
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
                if (q.selectColumns != null && !q.selectColumns.isEmpty()) {
                    for (String sc : q.selectColumns) {
                        if (!cols.contains(sc)) {
                            return QueryPlan.invalid("Column '" + sc + "' does not exist.");
                        }
                    }
                }
                int rowCount = tables.get(q.tableName).size();
                boolean indexCandidate = q.whereColumns.size() == 1
                    && q.whereOps.size() == 1
                    && "=".equals(q.whereOps.get(0));
                QueryPlan.ScanType scanType = indexCandidate
                    ? QueryPlan.ScanType.INDEX_SCAN
                    : QueryPlan.ScanType.FULL_TABLE_SCAN;
                int estimated = indexCandidate ? Math.max(1, rowCount / 10) : rowCount;
                String desc = q.whereColumns.isEmpty()
                    ? "Full scan of '" + q.tableName + "' (" + rowCount + " rows)"
                    : (indexCandidate
                        ? "Index candidate scan on '" + q.whereColumns.get(0) + "'"
                        : "Full scan of '" + q.tableName + "' with filter on " + q.whereColumns);
                return QueryPlan.valid("SELECT", q.tableName, scanType, estimated, desc);
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
                boolean indexCandidate = q.whereColumns.size() == 1
                    && q.whereOps.size() == 1
                    && "=".equals(q.whereOps.get(0));
                return QueryPlan.valid("UPDATE", q.tableName,
                    indexCandidate ? QueryPlan.ScanType.INDEX_SCAN : QueryPlan.ScanType.FULL_TABLE_SCAN,
                    indexCandidate ? Math.max(1, rowCount / 10) : rowCount,
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
                boolean indexCandidate = q.whereColumns.size() == 1
                    && q.whereOps.size() == 1
                    && "=".equals(q.whereOps.get(0));
                return QueryPlan.valid("DELETE", q.tableName,
                    indexCandidate ? QueryPlan.ScanType.INDEX_SCAN : QueryPlan.ScanType.FULL_TABLE_SCAN,
                    indexCandidate ? Math.max(1, rowCount / 10) : rowCount,
                    "Scan '" + q.tableName + "' and delete matching rows");
            }

            default:
                return QueryPlan.invalid("Unsupported query type: " + q.type);
        }
    }
}
