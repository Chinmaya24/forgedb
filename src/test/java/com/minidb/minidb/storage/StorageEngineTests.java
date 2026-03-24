package com.minidb.minidb.storage;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageEngineTests {

    @Test
    void writesSchemaTypeFile() {
        Map<String, List<String>> schemaTypes = new HashMap<>();
        schemaTypes.put("users", List.of("INT", "TEXT"));
        StorageEngine.saveTypes(schemaTypes);

        File typeFile = new File("data/schema_types.json");
        assertTrue(typeFile.exists());
    }
}
