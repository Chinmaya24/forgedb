package com.minidb.minidb.engine;

import com.minidb.minidb.btree.BTree;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class IndexManager {

    private static final String INDEX_DIR = "data/indexes/";
    private static final ObjectMapper mapper = new ObjectMapper();

    // tableName -> columnName -> BTree index
    private Map<String, Map<String, BTree>> indexes = new HashMap<>();
    // tableName -> list of indexed columns
    private Map<String, List<String>> indexMeta = new HashMap<>();

    public IndexManager() {
        new File(INDEX_DIR).mkdirs();
        loadMeta();
    }

    // Create index on a column
    public void createIndex(String tableName, String columnName,
                            List<String> columns, List<List<String>> rows) {
        int colIdx = columns.indexOf(columnName);
        if (colIdx == -1) return;

        BTree tree = new BTree();
        for (List<String> row : rows) {
            tree.insert(row.get(colIdx), row);
        }

        indexes.computeIfAbsent(tableName, k -> new HashMap<>()).put(columnName, tree);
        indexMeta.computeIfAbsent(tableName, k -> new ArrayList<>()).add(columnName);
        saveMeta();
    }

    // Drop index on a column
    public void dropIndex(String tableName, String columnName) {
        if (indexes.containsKey(tableName)) {
            indexes.get(tableName).remove(columnName);
        }
        if (indexMeta.containsKey(tableName)) {
            indexMeta.get(tableName).remove(columnName);
        }
        saveMeta();
    }

    // Drop all indexes for a table
    public void dropAllIndexes(String tableName) {
        indexes.remove(tableName);
        indexMeta.remove(tableName);
        saveMeta();
    }

    // Check if index exists
    public boolean hasIndex(String tableName, String columnName) {
        return indexes.containsKey(tableName) &&
               indexes.get(tableName).containsKey(columnName);
    }

    // Search using index
    public List<String> searchIndex(String tableName, String columnName, String value) {
        if (!hasIndex(tableName, columnName)) return null;
        return indexes.get(tableName).get(columnName).search(value);
    }

    // Get all indexed columns for a table
    public List<String> getIndexes(String tableName) {
        return indexMeta.getOrDefault(tableName, new ArrayList<>());
    }

    // Add row to all indexes for a table
    public void indexRow(String tableName, List<String> columns, List<String> row) {
        if (!indexes.containsKey(tableName)) return;
        for (Map.Entry<String, BTree> entry : indexes.get(tableName).entrySet()) {
            int colIdx = columns.indexOf(entry.getKey());
            if (colIdx >= 0 && colIdx < row.size()) {
                entry.getValue().insert(row.get(colIdx), row);
            }
        }
    }

    // Remove row from all indexes
    public void removeFromIndexes(String tableName, List<String> columns, List<String> row) {
        if (!indexes.containsKey(tableName)) return;
        for (Map.Entry<String, BTree> entry : indexes.get(tableName).entrySet()) {
            int colIdx = columns.indexOf(entry.getKey());
            if (colIdx >= 0 && colIdx < row.size()) {
                entry.getValue().delete(row.get(colIdx));
            }
        }
    }

    private void saveMeta() {
        try {
            mapper.writeValue(new File(INDEX_DIR + "meta.json"), indexMeta);
        } catch (IOException e) {
            System.out.println("Index meta save error: " + e.getMessage());
        }
    }

    private void loadMeta() {
        try {
            File f = new File(INDEX_DIR + "meta.json");
            if (!f.exists()) return;
            indexMeta = mapper.readValue(f,
                new TypeReference<Map<String, List<String>>>() {});
        } catch (IOException e) {
            System.out.println("Index meta load error: " + e.getMessage());
        }
    }
}
