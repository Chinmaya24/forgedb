package com.minidb.minidb.engine;

import java.util.*;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.storage.StorageEngine;

public class QueryExecutor {

    private static Map<String, List<String>> schemas = new HashMap<>();
    private static Map<String, List<List<String>>> tables = new HashMap<>();

    // Load data from disk when engine starts
    static {
        StorageEngine.load(schemas, tables);
    }

    public String execute(Query q) {

        if (q.type.equalsIgnoreCase("CREATE")) {
            if (schemas.containsKey(q.tableName)) {
                return "Error: Table '" + q.tableName + "' already exists.";
            }
            schemas.put(q.tableName, q.columns);
            tables.put(q.tableName, new ArrayList<>());
            StorageEngine.save(schemas, tables);
            return "Table '" + q.tableName + "' created with columns: " + q.columns;
        }

        if (q.type.equalsIgnoreCase("INSERT")) {
            if (!schemas.containsKey(q.tableName)) {
                return "Error: Table '" + q.tableName + "' does not exist. Use CREATE TABLE first.";
            }
            tables.get(q.tableName).add(q.values);
            StorageEngine.save(schemas, tables);
            return "Inserted into " + q.tableName + ": " + q.values;
        }

        if (q.type.equalsIgnoreCase("SELECT")) {
            if (!schemas.containsKey(q.tableName)) {
                return "Error: Table '" + q.tableName + "' does not exist.";
            }

            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);

            int whereIdx = -1;
            if (q.whereColumn != null) {
                whereIdx = columns.indexOf(q.whereColumn);
                if (whereIdx == -1) {
                    return "Error: Column '" + q.whereColumn + "' does not exist.";
                }
            }

            StringBuilder result = new StringBuilder();
            result.append(String.join(" | ", columns)).append("\n");
            result.append("-".repeat(30)).append("\n");

            boolean anyRows = false;
            for (List<String> row : rows) {
                if (whereIdx != -1) {
                    if (!row.get(whereIdx).trim().equalsIgnoreCase(q.whereValue.trim())) {
                        continue;
                    }
                }
                result.append(String.join(" | ", row)).append("\n");
                anyRows = true;
            }

            if (!anyRows) {
                return "No rows found.";
            }

            return result.toString();
        }

        if (q.type.equalsIgnoreCase("DELETE")) {
            if (!schemas.containsKey(q.tableName)) {
                return "Error: Table '" + q.tableName + "' does not exist.";
            }

            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);

            if (q.whereColumn == null) {
                int count = rows.size();
                rows.clear();
                StorageEngine.save(schemas, tables);
                return "Deleted " + count + " row(s) from " + q.tableName + ".";
            }

            int whereIdx = columns.indexOf(q.whereColumn);
            if (whereIdx == -1) {
                return "Error: Column '" + q.whereColumn + "' does not exist.";
            }

            int count = 0;
            Iterator<List<String>> iterator = rows.iterator();
            while (iterator.hasNext()) {
                List<String> row = iterator.next();
                if (row.get(whereIdx).trim().equalsIgnoreCase(q.whereValue.trim())) {
                    iterator.remove();
                    count++;
                }
            }

            if (count == 0) {
                return "No rows matched. Nothing deleted.";
            }

            StorageEngine.save(schemas, tables);
            return "Deleted " + count + " row(s) from " + q.tableName + ".";
        }

        if (q.type.equalsIgnoreCase("UPDATE")) {
            if (!schemas.containsKey(q.tableName)) {
                return "Error: Table '" + q.tableName + "' does not exist.";
            }

            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);

            int setIdx = columns.indexOf(q.setColumn);
            if (setIdx == -1) {
                return "Error: Column '" + q.setColumn + "' does not exist.";
            }

            int whereIdx = -1;
            if (q.whereColumn != null) {
                whereIdx = columns.indexOf(q.whereColumn);
                if (whereIdx == -1) {
                    return "Error: Column '" + q.whereColumn + "' does not exist.";
                }
            }

            int count = 0;
            for (List<String> row : rows) {
                if (whereIdx != -1) {
                    if (!row.get(whereIdx).trim().equalsIgnoreCase(q.whereValue.trim())) {
                        continue;
                    }
                }
                row.set(setIdx, q.setValue);
                count++;
            }

            if (count == 0) {
                return "No rows matched. Nothing updated.";
            }

            StorageEngine.save(schemas, tables);
            return "Updated " + count + " row(s) in " + q.tableName + ".";
        }

        return "Unsupported query";
    }
}
