package com.minidb.minidb.engine;

import java.util.*;
import com.minidb.minidb.btree.BTree;
import com.minidb.minidb.btree.BTreeStorage;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.planner.QueryPlan;
import com.minidb.minidb.planner.QueryPlanner;
import com.minidb.minidb.storage.StorageEngine;

public class QueryExecutor {

    private static Map<String, List<String>> schemas = new HashMap<>();
    private static Map<String, List<List<String>>> tables = new HashMap<>();
    private static Map<String, BTree> btrees = new HashMap<>();

    static {
        StorageEngine.load(schemas, tables);
        // Load BTrees for all existing tables
        for (String tableName : schemas.keySet()) {
            btrees.put(tableName, BTreeStorage.loadTree(tableName, 0));
        }
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

        QueryPlanner planner = new QueryPlanner(schemas, tables);
        QueryPlan plan = planner.plan(q);
        if (!plan.isValid) return "Error: " + plan.errorMessage;
        System.out.println("Query Plan: " + plan);

        if (q.type.equalsIgnoreCase("SHOW")) {
            if (schemas.isEmpty()) return "No tables found.";
            StringBuilder result = new StringBuilder();
            result.append("Tables\n").append("-".repeat(20)).append("\n");
            for (String t : schemas.keySet()) result.append(t).append("\n");
            return result.toString();
        }

        if (q.type.equalsIgnoreCase("CREATE")) {
            schemas.put(q.tableName, q.columns);
            tables.put(q.tableName, new ArrayList<>());
            btrees.put(q.tableName, new BTree());
            StorageEngine.save(schemas, tables);
            return "Table '" + q.tableName + "' created with columns: " + q.columns;
        }

        if (q.type.equalsIgnoreCase("DROP")) {
            schemas.remove(q.tableName);
            tables.remove(q.tableName);
            btrees.remove(q.tableName);
            BTreeStorage.deleteTree(q.tableName);
            StorageEngine.save(schemas, tables);
            return "Table '" + q.tableName + "' dropped successfully.";
        }

        if (q.type.equalsIgnoreCase("ALTER")) {
            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);
            BTree tree = btrees.get(q.tableName);

            if (q.alterAction.equalsIgnoreCase("ADD")) {
                columns.add(q.alterColumn);
                for (List<String> row : rows) row.add("");
                btrees.put(q.tableName, rebuildTree(q.tableName, rows));
                StorageEngine.save(schemas, tables);
                return "Column '" + q.alterColumn + "' added to " + q.tableName + ".";
            }
            if (q.alterAction.equalsIgnoreCase("DROP")) {
                int colIdx = columns.indexOf(q.alterColumn);
                columns.remove(colIdx);
                for (List<String> row : rows) {
                    if (colIdx < row.size()) row.remove(colIdx);
                }
                btrees.put(q.tableName, rebuildTree(q.tableName, rows));
                StorageEngine.save(schemas, tables);
                return "Column '" + q.alterColumn + "' removed from " + q.tableName + ".";
            }
        }

        if (q.type.equalsIgnoreCase("INSERT")) {
            List<String> row = new ArrayList<>(q.values);
            tables.get(q.tableName).add(row);
            btrees.get(q.tableName).insert(row.get(0), row);
            BTreeStorage.saveTree(q.tableName, tables.get(q.tableName));
            StorageEngine.save(schemas, tables);
            return "Inserted into " + q.tableName + ": " + row;
        }

        if (q.type.equalsIgnoreCase("SELECT")) {
            List<String> columns = schemas.get(q.tableName);
            BTree tree = btrees.get(q.tableName);

            // Use BTree for exact key lookup on first column
            List<List<String>> rows;
            if (q.whereColumns.size() == 1 &&
                q.whereColumns.get(0).equals(columns.get(0))) {
                System.out.println("Using BTree index lookup for key: " + q.whereValues.get(0));
                List<String> found = tree.search(q.whereValues.get(0));
                rows = found != null ? List.of(found) : new ArrayList<>();
            } else {
                rows = new ArrayList<>(tree.getAllRows());
            }

            if (q.orderByColumn != null) {
                int orderIdx = columns.indexOf(q.orderByColumn);
                rows = new ArrayList<>(rows);
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
            BTree tree = btrees.get(q.tableName);

            if (q.whereColumns.isEmpty()) {
                int count = rows.size();
                rows.clear();
                btrees.put(q.tableName, new BTree());
                BTreeStorage.saveTree(q.tableName, rows);
                StorageEngine.save(schemas, tables);
                return "Deleted " + count + " row(s) from " + q.tableName + ".";
            }

            int count = 0;
            Iterator<List<String>> iterator = rows.iterator();
            while (iterator.hasNext()) {
                List<String> row = iterator.next();
                if (matchesWhere(row, columns, q)) {
                    tree.delete(row.get(0));
                    iterator.remove();
                    count++;
                }
            }

            if (count == 0) return "No rows matched. Nothing deleted.";
            BTreeStorage.saveTree(q.tableName, rows);
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
                String key = row.get(0);
                row.set(setIdx, q.setValue);
                btrees.get(q.tableName).update(key, setIdx, q.setValue);
                count++;
            }

            if (count == 0) return "No rows matched. Nothing updated.";
            BTreeStorage.saveTree(q.tableName, rows);
            StorageEngine.save(schemas, tables);
            return "Updated " + count + " row(s) in " + q.tableName + ".";
        }

        return "Unsupported query";
    }

    private BTree rebuildTree(String tableName, List<List<String>> rows) {
        BTree tree = new BTree();
        for (List<String> row : rows) tree.insert(row.get(0), row);
        BTreeStorage.saveTree(tableName, rows);
        return tree;
    }
}
