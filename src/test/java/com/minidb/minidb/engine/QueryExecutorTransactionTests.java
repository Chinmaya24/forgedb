package com.minidb.minidb.engine;

import com.minidb.minidb.model.Query;
import com.minidb.minidb.parser.SQLParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorTransactionTests {

    private final SQLParser parser = new SQLParser();
    private final QueryExecutor executor = new QueryExecutor();

    @Test
    void beginRollbackDiscardsChanges() {
        String table = "tx_rb_users_" + System.nanoTime();
        Query begin = parser.parse("BEGIN");
        Query create = parser.parse("CREATE TABLE " + table + " (id INT, name TEXT)");
        Query insert = parser.parse("INSERT INTO " + table + " VALUES (1, 'alice')");
        Query rollback = parser.parse("ROLLBACK");
        Query select = parser.parse("SELECT * FROM " + table);

        executor.execute(begin);
        executor.execute(create);
        executor.execute(insert);
        executor.execute(rollback);
        String result = executor.execute(select);

        assertTrue(result.startsWith("Error: Table '" + table + "' does not exist."));
    }
}
