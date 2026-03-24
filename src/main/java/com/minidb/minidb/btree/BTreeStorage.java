package com.minidb.minidb.btree;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class BTreeStorage {

    private static final String DATA_DIR = "data/btree/";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void saveTree(String tableName, List<List<String>> rows) {
        try {
            new File(DATA_DIR).mkdirs();
            writeAtomic(DATA_DIR + tableName + ".json", rows);
        } catch (IOException e) {
            System.out.println("BTree save error: " + e.getMessage());
        }
    }

    public static BTree loadTree(String tableName, int pkIndex) {
        BTree tree = new BTree();
        try {
            File f = new File(DATA_DIR + tableName + ".json");
            if (!f.exists()) return tree;
            List<List<String>> rows = mapper.readValue(f,
                new TypeReference<List<List<String>>>() {});
            for (List<String> row : rows) {
                String key = row.get(pkIndex);
                tree.insert(key, row);
            }
        } catch (IOException e) {
            System.out.println("BTree load error: " + e.getMessage());
        }
        return tree;
    }

    public static void deleteTree(String tableName) {
        new File(DATA_DIR + tableName + ".json").delete();
    }

    private static void writeAtomic(String targetPath, Object value) throws IOException {
        Path target = Path.of(targetPath);
        Path dir = target.getParent();
        if (dir != null) Files.createDirectories(dir);
        Path tmp = Path.of(targetPath + ".tmp");
        mapper.writeValue(tmp.toFile(), value);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
