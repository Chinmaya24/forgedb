package com.minidb.minidb.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class StorageEngine {

    private static final String DATA_DIR = "data/";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void save(Map<String, List<String>> schemas,
                            Map<String, List<List<String>>> tables) {
        try {
            new File(DATA_DIR).mkdirs();
            mapper.writeValue(new File(DATA_DIR + "schemas.json"), schemas);
            mapper.writeValue(new File(DATA_DIR + "tables.json"), tables);
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
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
}
