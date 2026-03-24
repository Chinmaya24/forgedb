package com.minidb.minidb.parser;

import com.minidb.minidb.model.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLParserTests {

    private final SQLParser parser = new SQLParser();

    @Test
    void parsesSelectProjectionColumns() {
        Query q = parser.parse("SELECT id, name FROM users");
        assertEquals("SELECT", q.type);
        assertEquals("users", q.tableName);
        assertEquals(2, q.selectColumns.size());
        assertEquals("id", q.selectColumns.get(0));
        assertEquals("name", q.selectColumns.get(1));
    }

    @Test
    void parsesInsertWithColumnList() {
        Query q = parser.parse("INSERT INTO users (name, age) VALUES ('alice', 24)");
        assertEquals("INSERT", q.type);
        assertEquals("users", q.tableName);
        assertEquals(2, q.insertColumns.size());
        assertEquals("name", q.insertColumns.get(0));
        assertEquals("age", q.insertColumns.get(1));
        assertEquals(2, q.values.size());
    }

    @Test
    void parsesExtendedWhereOperators() {
        Query q = parser.parse("SELECT * FROM users WHERE age >= 18 AND age != 21");
        assertEquals("SELECT", q.type);
        assertEquals(2, q.whereOps.size());
        assertEquals(">=", q.whereOps.get(0));
        assertEquals("!=", q.whereOps.get(1));
        assertEquals(1, q.whereConnectors.size());
        assertEquals("AND", q.whereConnectors.get(0));
    }

    @Test
    void defaultsCreateColumnTypesWhenMissing() {
        Query q = parser.parse("CREATE TABLE users (id, name)");
        assertEquals("CREATE", q.type);
        assertEquals(2, q.columnTypes.size());
        assertTrue(q.columnTypes.stream().allMatch(t -> t.equals("TEXT")));
    }

    @Test
    void parsesTransactionCommands() {
        assertEquals("BEGIN", parser.parse("BEGIN").type);
        assertEquals("BEGIN", parser.parse("BEGIN TRANSACTION").type);
        assertEquals("COMMIT", parser.parse("COMMIT").type);
        assertEquals("ROLLBACK", parser.parse("ROLLBACK").type);
    }

    @Test
    void parsesMysqlStyleCreateTableWithConstraints() {
        Query q = parser.parse(
            "CREATE TABLE users (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "username VARCHAR(50) NOT NULL, " +
            "email VARCHAR(100) UNIQUE NOT NULL, " +
            "password VARCHAR(255) NOT NULL, " +
            "created_at TIMESTAMP NOT NULL)"
        );

        assertEquals("CREATE", q.type);
        assertEquals("users", q.tableName);
        assertEquals(5, q.columns.size());
        assertEquals("id", q.columns.get(0));
        assertEquals("username", q.columns.get(1));
        assertEquals("email", q.columns.get(2));
        assertEquals("password", q.columns.get(3));
        assertEquals("created_at", q.columns.get(4));
        assertEquals("INT", q.columnTypes.get(0));
        assertEquals("TEXT", q.columnTypes.get(1));
        assertEquals("TEXT", q.columnTypes.get(2));
        assertEquals("TEXT", q.columnTypes.get(3));
        assertEquals("TEXT", q.columnTypes.get(4));
        assertTrue(q.autoIncrementColumns.contains("id"));
    }
}
