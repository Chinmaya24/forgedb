package com.minidb.minidb.engine;

import com.minidb.minidb.parser.SQLParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorAutoIncrementTests {

    private final SQLParser parser = new SQLParser();
    private final QueryExecutor executor = new QueryExecutor();

    @Test
    void autoIncrementFillsMissingIdOnInsertColumnList() {
        String table = "tx_ai_users_" + System.nanoTime();
        executor.execute(parser.parse("CREATE TABLE " + table + " (id INT AUTO_INCREMENT, username TEXT)"));
        executor.execute(parser.parse("INSERT INTO " + table + " (username) VALUES ('alice')"));
        String result = executor.execute(parser.parse("SELECT id, username FROM " + table));
        assertTrue(result.contains("1 | alice"));
    }
}
