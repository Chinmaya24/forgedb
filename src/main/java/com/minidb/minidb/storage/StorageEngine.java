package com.minidb.minidb.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StorageEngine {

    private static final String DATA_DIR = "data/";
    private static final String TYPES_FILE = "schema_types.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void save(Map<String, List<String>> schemas,
                            Map<String, List<List<String>>> tables) {
        try {
            new File(DATA_DIR).mkdirs();
            writeAtomic(DATA_DIR + "schemas.json", schemas);
            writeAtomic(DATA_DIR + "tables.json", tables);
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    public static void saveTypes(Map<String, List<String>> schemaTypes) {
        try {
            new File(DATA_DIR).mkdirs();
            writeAtomic(DATA_DIR + TYPES_FILE, schemaTypes);
        } catch (IOException e) {
            System.out.println("Error saving schema types: " + e.getMessage());
        }
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

    public static void load(Map<String, List<String>> schemas,
                            Map<String, List<List<String>>> tables) {
        try {
            File schemaFile = new File(DATA_DIR + "schemas.json");
            File tableFile  = new File(DATA_DIR + "tables.json");

            if (schemaFile.exists()) {
                Map<String, List<String>> loadedSchemas = mapper.readValue(
                    schemaFile,
                    new TypeReference<Map<String, List<String>>>() {}
                );
                schemas.putAll(loadedSchemas);
            }

            if (tableFile.exists()) {
                Map<String, List<List<String>>> loadedTables = mapper.readValue(
                    tableFile,
                    new TypeReference<Map<String, List<List<String>>>>() {}
                );
                tables.putAll(loadedTables);
            }

            System.out.println("Data loaded from disk.");

        } catch (IOException e) {
            System.out.println("No existing data found, starting fresh.");
        }
    }

    public static void loadTypes(Map<String, List<String>> schemaTypes) {
        try {
            File typeFile = new File(DATA_DIR + TYPES_FILE);
            if (!typeFile.exists()) return;
            Map<String, List<String>> loadedTypes = mapper.readValue(
                typeFile,
                new TypeReference<Map<String, List<String>>>() {}
            );
            schemaTypes.putAll(loadedTypes);
        } catch (IOException e) {
            System.out.println("No schema types found, using defaults.");
        }
    }
}
