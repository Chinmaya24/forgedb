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
    private static Map<String, List<String>> schemaTypes = new HashMap<>();
    private static Map<String, Set<String>> autoIncrementColumnsByTable = new HashMap<>();
    private static Map<String, List<List<String>>> tables = new HashMap<>();
    private static Map<String, BTree> btrees = new HashMap<>();
    private static IndexManager indexManager = new IndexManager();
    private static boolean inTransaction = false;

    static {
        StorageEngine.load(schemas, tables);
        StorageEngine.loadTypes(schemaTypes);
        for (String tableName : schemas.keySet()) {
            btrees.put(tableName, BTreeStorage.loadTree(tableName, 0));
            schemaTypes.computeIfAbsent(tableName, t -> defaultTypesFor(schemas.get(tableName)));
        }
        indexManager.rebuildFromMeta(schemas, tables);
    }

    // Match a single condition (supports = and LIKE)
    private boolean matchCondition(List<String> row, List<String> columns, List<String> types, String col, String op, String val) {
        int colIdx = columns.indexOf(col);
        if (colIdx == -1) return false;
        String cellVal = row.get(colIdx).trim();
        String normalizedVal = val.trim();
        String colType = colIdx < types.size() ? types.get(colIdx) : "TEXT";
        if (op.equalsIgnoreCase("LIKE")) {
            String pattern = normalizedVal.replace("%", ".*").replace("_", ".");
            return cellVal.matches("(?i)" + pattern);
        }
        if (isNumericType(colType)) {
            try {
                double left = Double.parseDouble(cellVal);
                double right = Double.parseDouble(normalizedVal);
                switch (op) {
                    case "=": return Double.compare(left, right) == 0;
                    case "!=": return Double.compare(left, right) != 0;
                    case ">": return left > right;
                    case "<": return left < right;
                    case ">=": return left >= right;
                    case "<=": return left <= right;
                    default: return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        int cmp = cellVal.compareToIgnoreCase(normalizedVal);
        switch (op) {
            case "=": return cmp == 0;
            case "!=": return cmp != 0;
            case ">": return cmp > 0;
            case "<": return cmp < 0;
            case ">=": return cmp >= 0;
            case "<=": return cmp <= 0;
            default: return false;
        }
    }

    // Match all WHERE conditions respecting AND/OR
    private boolean matchesWhere(List<String> row, List<String> columns, List<String> types, Query q) {
        if (q.whereColumns.isEmpty()) return true;
        boolean result = matchCondition(row, columns, types, q.whereColumns.get(0), q.whereOps.get(0), q.whereValues.get(0));
        for (int i = 0; i < q.whereConnectors.size(); i++) {
            boolean next = matchCondition(row, columns, types, q.whereColumns.get(i + 1), q.whereOps.get(i + 1), q.whereValues.get(i + 1));
            if (q.whereConnectors.get(i).equalsIgnoreCase("AND")) result = result && next;
            else result = result || next;
        }
        return result;
    }

    private static List<String> defaultTypesFor(List<String> columns) {
        List<String> defaults = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) defaults.add("TEXT");
        return defaults;
    }

    private boolean isNumericType(String type) {
        String t = type.toUpperCase();
        return t.equals("INT") || t.equals("INTEGER") || t.equals("LONG") || t.equals("FLOAT") || t.equals("DOUBLE") || t.equals("DECIMAL");
    }

    private String normalizeValueForType(String rawValue, String type) {
        String t = type.toUpperCase();
        String value = rawValue.trim();
        if (t.equals("INT") || t.equals("INTEGER")) {
            Integer.parseInt(value);
            return value;
        }
        if (t.equals("LONG")) {
            Long.parseLong(value);
            return value;
        }
        if (t.equals("FLOAT") || t.equals("DOUBLE") || t.equals("DECIMAL")) {
            Double.parseDouble(value);
            return value;
        }
        return rawValue;
    }

    private String formatNumber(double val) {
        if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((long) val);
        return String.format("%.2f", val);
    }

    /**
     * Gets the current schemas map.
     *
     * @return map of table names to column lists
     */
    public Map<String, List<String>> getSchemas() {
        return schemas;
    }

    /**
     * Executes a parsed Query object and returns the result as a string.
     *
     * @param q the parsed Query object to execute
     * @return the execution result as a formatted string
     */
    public String execute(Query q) {
        if (q.type.equalsIgnoreCase("BEGIN")) {
            if (inTransaction) return "Error: Transaction already in progress.";
            inTransaction = true;
            indexManager.setAutoPersistMeta(false);
            return "Transaction started.";
        }

        if (q.type.equalsIgnoreCase("COMMIT")) {
            if (!inTransaction) return "Error: No active transaction.";
            persistAll();
            inTransaction = false;
            indexManager.setAutoPersistMeta(true);
            indexManager.flushMeta();
            return "Transaction committed.";
        }

        if (q.type.equalsIgnoreCase("ROLLBACK")) {
            if (!inTransaction) return "Error: No active transaction.";
            reloadFromDisk();
            inTransaction = false;
            indexManager.setAutoPersistMeta(true);
            return "Transaction rolled back.";
        }

        if (q.type.equalsIgnoreCase("AGGREGATE")) {
            if (!schemas.containsKey(q.tableName)) return "Error: Table '" + q.tableName + "' does not exist.";
            List<String> columns = schemas.get(q.tableName);
            List<String> types = schemaTypes.getOrDefault(q.tableName, defaultTypesFor(columns));
            List<List<String>> allRows = btrees.get(q.tableName).getAllRows();
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : allRows) {
                if (q.whereColumns.isEmpty() || matchesWhere(row, columns, types, q)) rows.add(row);
            }
            switch (q.aggregateFunction.toUpperCase()) {
                case "COUNT": return "COUNT(*) = " + rows.size();
                case "MAX": {
                    int ci = columns.indexOf(q.aggregateColumn);
                    if (ci == -1) return "Error: Column '" + q.aggregateColumn + "' does not exist.";
                    if (rows.isEmpty()) return "MAX(" + q.aggregateColumn + ") = null";
                    double max = Double.NEGATIVE_INFINITY;
                    for (List<String> row : rows) { try { max = Math.max(max, Double.parseDouble(row.get(ci).trim())); } catch(NumberFormatException e) { return "Error: MAX requires numeric column."; } }
                    return "MAX(" + q.aggregateColumn + ") = " + formatNumber(max);
                }
                case "MIN": {
                    int ci = columns.indexOf(q.aggregateColumn);
                    if (ci == -1) return "Error: Column '" + q.aggregateColumn + "' does not exist.";
                    if (rows.isEmpty()) return "MIN(" + q.aggregateColumn + ") = null";
                    double min = Double.POSITIVE_INFINITY;
                    for (List<String> row : rows) { try { min = Math.min(min, Double.parseDouble(row.get(ci).trim())); } catch(NumberFormatException e) { return "Error: MIN requires numeric column."; } }
                    return "MIN(" + q.aggregateColumn + ") = " + formatNumber(min);
                }
                case "SUM": {
                    int ci = columns.indexOf(q.aggregateColumn);
                    if (ci == -1) return "Error: Column '" + q.aggregateColumn + "' does not exist.";
                    double sum = 0;
                    for (List<String> row : rows) { try { sum += Double.parseDouble(row.get(ci).trim()); } catch(NumberFormatException e) { return "Error: SUM requires numeric column."; } }
                    return "SUM(" + q.aggregateColumn + ") = " + formatNumber(sum);
                }
                case "AVG": {
                    int ci = columns.indexOf(q.aggregateColumn);
                    if (ci == -1) return "Error: Column '" + q.aggregateColumn + "' does not exist.";
                    if (rows.isEmpty()) return "AVG(" + q.aggregateColumn + ") = null";
                    double sum = 0;
                    for (List<String> row : rows) { try { sum += Double.parseDouble(row.get(ci).trim()); } catch(NumberFormatException e) { return "Error: AVG requires numeric column."; } }
                    return "AVG(" + q.aggregateColumn + ") = " + formatNumber(sum / rows.size());
                }
                default: return "Error: Unknown aggregate: " + q.aggregateFunction;
            }
        }

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
            schemaTypes.put(q.tableName, q.columnTypes == null || q.columnTypes.isEmpty() ? defaultTypesFor(q.columns) : q.columnTypes);
            autoIncrementColumnsByTable.put(q.tableName, new HashSet<>(q.autoIncrementColumns));
            tables.put(q.tableName, new ArrayList<>());
            btrees.put(q.tableName, new BTree());
            persistAll();
            return "Table '" + q.tableName + "' created with columns: " + q.columns;
        }

        if (q.type.equalsIgnoreCase("DROP")) {
            schemas.remove(q.tableName);
            schemaTypes.remove(q.tableName);
            autoIncrementColumnsByTable.remove(q.tableName);
            tables.remove(q.tableName);
            btrees.remove(q.tableName);
            indexManager.dropAllIndexes(q.tableName);
            if (!inTransaction) BTreeStorage.deleteTree(q.tableName);
            persistAll();
            return "Table '" + q.tableName + "' dropped successfully.";
        }

        if (q.type.equalsIgnoreCase("ALTER")) {
            List<String> columns = schemas.get(q.tableName);
            List<String> types = schemaTypes.getOrDefault(q.tableName, defaultTypesFor(columns));
            List<List<String>> rows = tables.get(q.tableName);
            if (q.alterAction.equalsIgnoreCase("ADD")) {
                columns.add(q.alterColumn);
                types.add(q.alterType == null ? "TEXT" : q.alterType.toUpperCase());
                for (List<String> row : rows) row.add("");
                btrees.put(q.tableName, rebuildTree(q.tableName, rows));
                persistAll();
                return "Column '" + q.alterColumn + "' added to " + q.tableName + ".";
            }
            if (q.alterAction.equalsIgnoreCase("DROP")) {
                int colIdx = columns.indexOf(q.alterColumn);
                columns.remove(colIdx);
                if (colIdx < types.size()) types.remove(colIdx);
                for (List<String> row : rows) { if (colIdx < row.size()) row.remove(colIdx); }
                btrees.put(q.tableName, rebuildTree(q.tableName, rows));
                persistAll();
                return "Column '" + q.alterColumn + "' removed from " + q.tableName + ".";
            }
        }

        if (q.type.equalsIgnoreCase("INSERT")) {
            List<String> columns = schemas.get(q.tableName);
            if (columns == null) return "Error: Table '" + q.tableName + "' does not exist.";
            List<String> types = schemaTypes.getOrDefault(q.tableName, defaultTypesFor(columns));
            List<String> row = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) row.add("");

            try {
                if (q.insertColumns == null || q.insertColumns.isEmpty()) {
                    if (q.values.size() != columns.size()) {
                        return "Error: Column count mismatch: expected " + columns.size() + " values but got " + q.values.size() + ".";
                    }
                    for (int i = 0; i < columns.size(); i++) {
                        row.set(i, normalizeValueForType(q.values.get(i), types.get(i)));
                    }
                } else {
                    if (q.values.size() != q.insertColumns.size()) {
                        return "Error: Column list and values count mismatch.";
                    }
                    for (int i = 0; i < q.insertColumns.size(); i++) {
                        String col = q.insertColumns.get(i);
                        int idx = columns.indexOf(col);
                        if (idx == -1) return "Error: Column '" + col + "' does not exist.";
                        row.set(idx, normalizeValueForType(q.values.get(i), types.get(idx)));
                    }
                }
                applyAutoIncrementValues(q.tableName, columns, row);
            } catch (NumberFormatException e) {
                return "Error: Type mismatch for inserted value.";
            }
            tables.get(q.tableName).add(row);
            btrees.get(q.tableName).insert(row.get(0), row);
            indexManager.indexRow(q.tableName, columns, row);
            persistTable(q.tableName);
            persistAll();
            return "Inserted into " + q.tableName + ": " + row;
        }

        if (q.type.equalsIgnoreCase("SELECT")) {
            List<String> columns = schemas.get(q.tableName);
            List<String> types = schemaTypes.getOrDefault(q.tableName, defaultTypesFor(columns));

            // JOIN
            if (q.joinTable != null) {
                if (!schemas.containsKey(q.joinTable)) return "Error: Table '" + q.joinTable + "' does not exist.";
                List<String> rightCols = schemas.get(q.joinTable);
                List<List<String>> leftRows  = btrees.get(q.tableName).getAllRows();
                List<List<String>> rightRows = btrees.get(q.joinTable).getAllRows();
                String lc = q.joinLeftCol.contains(".") ? q.joinLeftCol.split("\\.")[1] : q.joinLeftCol;
                String rc = q.joinRightCol.contains(".") ? q.joinRightCol.split("\\.")[1] : q.joinRightCol;
                int li = columns.indexOf(lc), ri = rightCols.indexOf(rc);
                if (li == -1) return "Error: Column '" + lc + "' not found in " + q.tableName;
                if (ri == -1) return "Error: Column '" + rc + "' not found in " + q.joinTable;
                List<String> combinedCols = new ArrayList<>();
                for (String c : columns) combinedCols.add(q.tableName + "." + c);
                for (String c : rightCols) combinedCols.add(q.joinTable + "." + c);
                StringBuilder result = new StringBuilder();
                result.append(String.join(" | ", combinedCols)).append("\n").append("-".repeat(50)).append("\n");
                boolean any = false;
                for (List<String> lr : leftRows) {
                    for (List<String> rr : rightRows) {
                        if (lr.get(li).trim().equalsIgnoreCase(rr.get(ri).trim())) {
                            List<String> combined = new ArrayList<>(lr);
                            combined.addAll(rr);
                            result.append(String.join(" | ", combined)).append("\n");
                            any = true;
                        }
                    }
                }
                if (!any) return "No matching rows found.";
                return result.toString();
            }

            // Get rows - use index/btree if single equality WHERE on indexed col
            List<List<String>> rows;
            boolean canUseIndex = q.whereColumns.size() == 1
                && q.whereOps.size() > 0
                && q.whereOps.get(0).equals("=")
                && q.whereConnectors.isEmpty();

            if (canUseIndex && indexManager.hasIndex(q.tableName, q.whereColumns.get(0))) {
                System.out.println("Using INDEX on " + q.whereColumns.get(0));
                rows = new ArrayList<>(indexManager.searchAllIndex(
                    q.tableName,
                    q.whereColumns.get(0),
                    q.whereValues.get(0),
                    columns,
                    tables.get(q.tableName)
                ));
            } else if (canUseIndex && q.whereColumns.get(0).equals(columns.get(0))) {
                System.out.println("Using BTree primary key lookup");
                List<String> found = btrees.get(q.tableName).search(q.whereValues.get(0));
                rows = found != null ? new ArrayList<>(List.of(found)) : new ArrayList<>();
            } else {
                rows = new ArrayList<>(btrees.get(q.tableName).getAllRows());
            }

            // Apply WHERE filter
            if (!q.whereColumns.isEmpty()) {
                List<List<String>> filtered = new ArrayList<>();
                for (List<String> row : rows) {
                    if (matchesWhere(row, columns, types, q)) filtered.add(row);
                }
                rows = filtered;
            }

            // ORDER BY with direction
            if (q.orderByColumn != null) {
                int orderIdx = columns.indexOf(q.orderByColumn);
                if (orderIdx != -1) {
                    final boolean desc = q.orderByDirection.equalsIgnoreCase("DESC");
                    rows.sort((a, b) -> {
                        String va = a.get(orderIdx).trim();
                        String vb = b.get(orderIdx).trim();
                        int cmp;
                        try { cmp = Double.compare(Double.parseDouble(va), Double.parseDouble(vb)); }
                        catch (NumberFormatException e) { cmp = va.compareToIgnoreCase(vb); }
                        return desc ? -cmp : cmp;
                    });
                }
            }

            // LIMIT + OFFSET
            if (q.offset > 0) rows = rows.subList(Math.min(q.offset, rows.size()), rows.size());
            if (q.limit >= 0) rows = rows.subList(0, Math.min(q.limit, rows.size()));

            StringBuilder result = new StringBuilder();
            List<String> displayColumns = (q.selectColumns == null || q.selectColumns.isEmpty()) ? columns : q.selectColumns;
            List<Integer> selectIndexes = new ArrayList<>();
            for (String c : displayColumns) {
                int idx = columns.indexOf(c);
                if (idx == -1) return "Error: Column '" + c + "' does not exist.";
                selectIndexes.add(idx);
            }
            result.append(String.join(" | ", displayColumns)).append("\n").append("-".repeat(30)).append("\n");
            if (rows.isEmpty()) return "No rows found.";
            for (List<String> row : rows) {
                List<String> projected = new ArrayList<>();
                for (int idx : selectIndexes) projected.add(row.get(idx));
                result.append(String.join(" | ", projected)).append("\n");
            }
            return result.toString();
        }

        if (q.type.equalsIgnoreCase("DELETE")) {
            List<String> columns = schemas.get(q.tableName);
            List<String> types = schemaTypes.getOrDefault(q.tableName, defaultTypesFor(columns));
            List<List<String>> rows = tables.get(q.tableName);
            if (q.whereColumns.isEmpty()) {
                int count = rows.size(); rows.clear();
                btrees.put(q.tableName, new BTree());
                persistTable(q.tableName);
                persistAll();
                return "Deleted " + count + " row(s) from " + q.tableName + ".";
            }
            int count = 0;
            Iterator<List<String>> it = rows.iterator();
            while (it.hasNext()) {
                List<String> row = it.next();
                if (matchesWhere(row, columns, types, q)) {
                    btrees.get(q.tableName).delete(row.get(0));
                    indexManager.removeFromIndexes(q.tableName, columns, row);
                    it.remove(); count++;
                }
            }
            if (count == 0) return "No rows matched. Nothing deleted.";
            persistTable(q.tableName);
            persistAll();
            return "Deleted " + count + " row(s) from " + q.tableName + ".";
        }

        if (q.type.equalsIgnoreCase("UPDATE")) {
            List<String> columns = schemas.get(q.tableName);
            List<String> types = schemaTypes.getOrDefault(q.tableName, defaultTypesFor(columns));
            List<List<String>> rows = tables.get(q.tableName);
            int setIdx = columns.indexOf(q.setColumn);
            if (setIdx == -1) return "Error: Column '" + q.setColumn + "' does not exist.";
            try {
                q.setValue = normalizeValueForType(q.setValue, types.get(setIdx));
            } catch (NumberFormatException e) {
                return "Error: Type mismatch for update value.";
            }
            int count = 0;
            for (List<String> row : rows) {
                if (!q.whereColumns.isEmpty() && !matchesWhere(row, columns, types, q)) continue;
                indexManager.removeFromIndexes(q.tableName, columns, row);
                row.set(setIdx, q.setValue);
                btrees.get(q.tableName).update(row.get(0), setIdx, q.setValue);
                indexManager.indexRow(q.tableName, columns, row);
                count++;
            }
            if (count == 0) return "No rows matched. Nothing updated.";
            persistTable(q.tableName);
            persistAll();
            return "Updated " + count + " row(s) in " + q.tableName + ".";
        }

        return "Unsupported query";
    }

    private BTree rebuildTree(String tableName, List<List<String>> rows) {
        BTree tree = new BTree();
        for (List<String> row : rows) tree.insert(row.get(0), row);
        persistTable(tableName);
        return tree;
    }

    private void persistTable(String tableName) {
        if (inTransaction) return;
        BTreeStorage.saveTree(tableName, tables.get(tableName));
    }

    private void persistAll() {
        if (inTransaction) return;
        StorageEngine.save(schemas, tables);
        StorageEngine.saveTypes(schemaTypes);
        for (String tableName : tables.keySet()) {
            BTreeStorage.saveTree(tableName, tables.get(tableName));
        }
        indexManager.flushMeta();
    }

    private void reloadFromDisk() {
        schemas.clear();
        schemaTypes.clear();
        autoIncrementColumnsByTable.clear();
        tables.clear();
        btrees.clear();
        StorageEngine.load(schemas, tables);
        StorageEngine.loadTypes(schemaTypes);
        for (String tableName : schemas.keySet()) {
            btrees.put(tableName, BTreeStorage.loadTree(tableName, 0));
            schemaTypes.computeIfAbsent(tableName, t -> defaultTypesFor(schemas.get(tableName)));
        }
        indexManager = new IndexManager();
        indexManager.rebuildFromMeta(schemas, tables);
    }

    private void applyAutoIncrementValues(String tableName, List<String> columns, List<String> row) {
        Set<String> autoCols = autoIncrementColumnsByTable.getOrDefault(tableName, Collections.emptySet());
        if (autoCols.isEmpty()) return;
        List<List<String>> existingRows = tables.getOrDefault(tableName, Collections.emptyList());
        for (String col : autoCols) {
            int idx = columns.indexOf(col);
            if (idx < 0 || idx >= row.size()) continue;
            String current = row.get(idx);
            if (current != null && !current.trim().isEmpty()) continue;
            long nextValue = 1;
            for (List<String> existing : existingRows) {
                if (idx >= existing.size()) continue;
                String v = existing.get(idx);
                if (v == null || v.trim().isEmpty()) continue;
                try {
                    nextValue = Math.max(nextValue, Long.parseLong(v.trim()) + 1);
                } catch (NumberFormatException ignored) {
                }
            }
            row.set(idx, String.valueOf(nextValue));
        }
    }
}
