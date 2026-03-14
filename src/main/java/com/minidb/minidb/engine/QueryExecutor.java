package com.minidb.minidb.engine;

import java.util.*;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.planner.QueryPlan;
import com.minidb.minidb.planner.QueryPlanner;
import com.minidb.minidb.storage.StorageEngine;

public class QueryExecutor {

    private static Map<String, List<String>> schemas = new HashMap<>();
    private static Map<String, List<List<String>>> tables = new HashMap<>();

    static {
        StorageEngine.load(schemas, tables);
    }

    private boolean matchesWhere(List<String> row, List<String> columns, Query q) {
        for (int i = 0; i < q.whereColumns.size(); i++) {
            int colIdx = columns.indexOf(q.whereColumns.get(i));
            if (colIdx == -1) return false;
            if (!row.get(colIdx).trim().equalsIgnoreCase(q.whereValues.get(i).trim())) {
                return false;
            }
        }
        return true;
    }

    public String execute(Query q) {

        // Run query planner first
        QueryPlanner planner = new QueryPlanner(schemas, tables);
        QueryPlan plan = planner.plan(q);

        // If plan is invalid, return error immediately
        if (!plan.isValid) {
            return "Error: " + plan.errorMessage;
        }

        // Log the plan to console
        System.out.println("Query Plan: " + plan);

        if (q.type.equalsIgnoreCase("SHOW")) {
            if (schemas.isEmpty()) return "No tables found.";
            StringBuilder result = new StringBuilder();
            result.append("Tables\n").append("-".repeat(20)).append("\n");
            for (String tableName : schemas.keySet()) {
                result.append(tableName).append("\n");
            }
            return result.toString();
        }

        if (q.type.equalsIgnoreCase("CREATE")) {
            schemas.put(q.tableName, q.columns);
            tables.put(q.tableName, new ArrayList<>());
            StorageEngine.save(schemas, tables);
            return "Table '" + q.tableName + "' created with columns: " + q.columns;
        }

        if (q.type.equalsIgnoreCase("DROP")) {
            schemas.remove(q.tableName);
            tables.remove(q.tableName);
            StorageEngine.save(schemas, tables);
            return "Table '" + q.tableName + "' dropped successfully.";
        }

        if (q.type.equalsIgnoreCase("ALTER")) {
            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);

            if (q.alterAction.equalsIgnoreCase("ADD")) {
                columns.add(q.alterColumn);
                for (List<String> row : rows) row.add("");
                StorageEngine.save(schemas, tables);
                return "Column '" + q.alterColumn + "' added to " + q.tableName + ".";
            }
            if (q.alterAction.equalsIgnoreCase("DROP")) {
                int colIdx = columns.indexOf(q.alterColumn);
                columns.remove(colIdx);
                for (List<String> row : rows) {
                    if (colIdx < row.size()) row.remove(colIdx);
                }
                StorageEngine.save(schemas, tables);
                return "Column '" + q.alterColumn + "' removed from " + q.tableName + ".";
            }
        }

        if (q.type.equalsIgnoreCase("INSERT")) {
            tables.get(q.tableName).add(q.values);
            StorageEngine.save(schemas, tables);
            return "Inserted into " + q.tableName + ": " + q.values;
        }

        if (q.type.equalsIgnoreCase("SELECT")) {
            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = new ArrayList<>(tables.get(q.tableName));

            if (q.orderByColumn != null) {
                int orderIdx = columns.indexOf(q.orderByColumn);
                rows.sort((a, b) -> {
                    String va = a.get(orderIdx).trim();
                    String vb = b.get(orderIdx).trim();
                    try {
                        return Double.compare(Double.parseDouble(va), Double.parseDouble(vb));
                    } catch (NumberFormatException e) {
                        return va.compareToIgnoreCase(vb);
                    }
                });
            }

            StringBuilder result = new StringBuilder();
            result.append(String.join(" | ", columns)).append("\n");
            result.append("-".repeat(30)).append("\n");

            boolean anyRows = false;
            for (List<String> row : rows) {
                if (!q.whereColumns.isEmpty() && !matchesWhere(row, columns, q)) continue;
                result.append(String.join(" | ", row)).append("\n");
                anyRows = true;
            }

            if (!anyRows) return "No rows found.";
            return result.toString();
        }

        if (q.type.equalsIgnoreCase("DELETE")) {
            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);

            if (q.whereColumns.isEmpty()) {
                int count = rows.size();
                rows.clear();
                StorageEngine.save(schemas, tables);
                return "Deleted " + count + " row(s) from " + q.tableName + ".";
            }

            int count = 0;
            Iterator<List<String>> iterator = rows.iterator();
            while (iterator.hasNext()) {
                List<String> row = iterator.next();
                if (matchesWhere(row, columns, q)) {
                    iterator.remove();
                    count++;
                }
            }

            if (count == 0) return "No rows matched. Nothing deleted.";
            StorageEngine.save(schemas, tables);
            return "Deleted " + count + " row(s) from " + q.tableName + ".";
        }

        if (q.type.equalsIgnoreCase("UPDATE")) {
            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);
            int setIdx = columns.indexOf(q.setColumn);

            int count = 0;
            for (List<String> row : rows) {
                if (!q.whereColumns.isEmpty() && !matchesWhere(row, columns, q)) continue;
                row.set(setIdx, q.setValue);
                count++;
            }

            if (count == 0) return "No rows matched. Nothing updated.";
            StorageEngine.save(schemas, tables);
            return "Updated " + count + " row(s) in " + q.tableName + ".";
        }

        return "Unsupported query";
    }
}
