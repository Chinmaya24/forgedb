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
    private static IndexManager indexManager = new IndexManager();

    static {
        StorageEngine.load(schemas, tables);
        for (String tableName : schemas.keySet()) {
            btrees.put(tableName, BTreeStorage.loadTree(tableName, 0));
        }
    }

    private boolean matchesWhere(List<String> row, List<String> columns, Query q) {
        for (int i = 0; i < q.whereColumns.size(); i++) {
            int colIdx = columns.indexOf(q.whereColumns.get(i));
            if (colIdx == -1) return false;
            if (!row.get(colIdx).trim().equalsIgnoreCase(q.whereValues.get(i).trim())) return false;
        }
        return true;
    }

    public String execute(Query q) {

        if (q.type.equalsIgnoreCase("CREATE_INDEX")) {
            if (!schemas.containsKey(q.tableName)) return "Error: Table '" + q.tableName + "' does not exist.";
            List<String> columns = schemas.get(q.tableName);
            if (!columns.contains(q.indexColumn)) return "Error: Column '" + q.indexColumn + "' does not exist.";
            if (indexManager.hasIndex(q.tableName, q.indexColumn)) return "Error: Index on '" + q.indexColumn + "' already exists.";
            indexManager.createIndex(q.tableName, q.indexColumn, columns, tables.get(q.tableName));
            return "Index created on " + q.tableName + "(" + q.indexColumn + ").";
        }

        if (q.type.equalsIgnoreCase("DROP_INDEX")) {
            if (!indexManager.hasIndex(q.tableName, q.indexColumn)) return "Error: No index on '" + q.indexColumn + "'.";
            indexManager.dropIndex(q.tableName, q.indexColumn);
            return "Index on " + q.tableName + "(" + q.indexColumn + ") dropped.";
        }

        if (q.type.equalsIgnoreCase("SHOW_INDEXES")) {
            if (!schemas.containsKey(q.tableName)) return "Error: Table '" + q.tableName + "' does not exist.";
            List<String> idxList = indexManager.getIndexes(q.tableName);
            if (idxList.isEmpty()) return "No indexes on '" + q.tableName + "'.";
            StringBuilder sb = new StringBuilder();
            sb.append("Indexes on ").append(q.tableName).append("\n").append("-".repeat(20)).append("\n");
            for (String col : idxList) sb.append("INDEX ON ").append(q.tableName).append("(").append(col).append(")\n");
            return sb.toString();
        }

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
            indexManager.dropAllIndexes(q.tableName);
            BTreeStorage.deleteTree(q.tableName);
            StorageEngine.save(schemas, tables);
            return "Table '" + q.tableName + "' dropped successfully.";
        }

        if (q.type.equalsIgnoreCase("ALTER")) {
            List<String> columns = schemas.get(q.tableName);
            List<List<String>> rows = tables.get(q.tableName);
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
                for (List<String> row : rows) { if (colIdx < row.size()) row.remove(colIdx); }
                btrees.put(q.tableName, rebuildTree(q.tableName, rows));
                StorageEngine.save(schemas, tables);
                return "Column '" + q.alterColumn + "' removed from " + q.tableName + ".";
            }
        }

        if (q.type.equalsIgnoreCase("INSERT")) {
            List<String> row = new ArrayList<>(q.values);
            tables.get(q.tableName).add(row);
            btrees.get(q.tableName).insert(row.get(0), row);
            indexManager.indexRow(q.tableName, schemas.get(q.tableName), row);
            BTreeStorage.saveTree(q.tableName, tables.get(q.tableName));
            StorageEngine.save(schemas, tables);
            return "Inserted into " + q.tableName + ": " + row;
        }

        if (q.type.equalsIgnoreCase("SELECT")) {
            List<String> leftCols = schemas.get(q.tableName);

            // JOIN query
            if (q.joinTable != null) {
                if (!schemas.containsKey(q.joinTable)) {
                    return "Error: Table '" + q.joinTable + "' does not exist.";
                }

                List<String> rightCols = schemas.get(q.joinTable);
                List<List<String>> leftRows  = btrees.get(q.tableName).getAllRows();
                List<List<String>> rightRows = btrees.get(q.joinTable).getAllRows();

                // Parse join columns: table.column ? get column name only
                String leftColName  = q.joinLeftCol.contains(".")  ? q.joinLeftCol.split("\\.")[1]  : q.joinLeftCol;
                String rightColName = q.joinRightCol.contains(".") ? q.joinRightCol.split("\\.")[1] : q.joinRightCol;

                int leftIdx  = leftCols.indexOf(leftColName);
                int rightIdx = rightCols.indexOf(rightColName);

                if (leftIdx  == -1) return "Error: Column '" + leftColName  + "' not found in " + q.tableName;
                if (rightIdx == -1) return "Error: Column '" + rightColName + "' not found in " + q.joinTable;

                // Build combined headers
                List<String> combinedCols = new ArrayList<>();
                for (String c : leftCols)  combinedCols.add(q.tableName  + "." + c);
                for (String c : rightCols) combinedCols.add(q.joinTable + "." + c);

                StringBuilder result = new StringBuilder();
                result.append(String.join(" | ", combinedCols)).append("\n");
                result.append("-".repeat(50)).append("\n");

                boolean anyRows = false;
                for (List<String> leftRow : leftRows) {
                    for (List<String> rightRow : rightRows) {
                        if (leftRow.get(leftIdx).trim().equalsIgnoreCase(rightRow.get(rightIdx).trim())) {
                            List<String> combined = new ArrayList<>();
                            combined.addAll(leftRow);
                            combined.addAll(rightRow);
                            result.append(String.join(" | ", combined)).append("\n");
                            anyRows = true;
                        }
                    }
                }

                if (!anyRows) return "No matching rows found.";
                return result.toString();
            }

            // Regular SELECT
            List<List<String>> rows;
            if (q.whereColumns.size() >= 1) {
                String whereCol = q.whereColumns.get(0);
                String whereVal = q.whereValues.get(0);
                if (indexManager.hasIndex(q.tableName, whereCol)) {
                    System.out.println("Using INDEX on " + whereCol);
                    List<String> found = indexManager.searchIndex(q.tableName, whereCol, whereVal);
                    rows = found != null ? new ArrayList<>(List.of(found)) : new ArrayList<>();
                } else if (whereCol.equals(leftCols.get(0))) {
                    System.out.println("Using BTree primary key lookup");
                    List<String> found = btrees.get(q.tableName).search(whereVal);
                    rows = found != null ? new ArrayList<>(List.of(found)) : new ArrayList<>();
                } else {
                    rows = new ArrayList<>(btrees.get(q.tableName).getAllRows());
                }
            } else {
                rows = new ArrayList<>(btrees.get(q.tableName).getAllRows());
            }

            if (q.orderByColumn != null) {
                int orderIdx = leftCols.indexOf(q.orderByColumn);
                rows.sort((a, b) -> {
                    String va = a.get(orderIdx).trim();
                    String vb = b.get(orderIdx).trim();
                    try { return Double.compare(Double.parseDouble(va), Double.parseDouble(vb)); }
                    catch (NumberFormatException e) { return va.compareToIgnoreCase(vb); }
                });
            }

            StringBuilder result = new StringBuilder();
            result.append(String.join(" | ", leftCols)).append("\n");
            result.append("-".repeat(30)).append("\n");

            boolean anyRows = false;
            for (List<String> row : rows) {
                if (!q.whereColumns.isEmpty() && !matchesWhere(row, leftCols, q)) continue;
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
                    btrees.get(q.tableName).delete(row.get(0));
                    indexManager.removeFromIndexes(q.tableName, columns, row);
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
                indexManager.removeFromIndexes(q.tableName, columns, row);
                row.set(setIdx, q.setValue);
                btrees.get(q.tableName).update(row.get(0), setIdx, q.setValue);
                indexManager.indexRow(q.tableName, columns, row);
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
