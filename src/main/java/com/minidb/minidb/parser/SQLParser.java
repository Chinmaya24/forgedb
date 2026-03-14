package com.minidb.minidb.parser;

import java.util.ArrayList;
import java.util.List;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.tokenizer.Lexer;
import com.minidb.minidb.tokenizer.Token;
import com.minidb.minidb.tokenizer.TokenType;

public class SQLParser {

    private List<Token> tokens;
    private int pos;

    public Query parse(String sql) {
        Lexer lexer = new Lexer(sql);
        this.tokens = lexer.tokenize();
        this.pos = 0;

        Query q = new Query();
        Token first = peek();

        switch (first.type) {
            case SELECT: return parseSelect();
            case INSERT: return parseInsert();
            case UPDATE: return parseUpdate();
            case DELETE: return parseDelete();
            case CREATE: return parseCreate();
            case DROP:   return parseDrop();
            case ALTER:  return parseAlter();
            case SHOW:   return parseShow();
            default:
                q.type = "UNKNOWN";
                return q;
        }
    }

    // SELECT * FROM <table> [WHERE ...] [ORDER BY ...]
    private Query parseSelect() {
        Query q = new Query();
        q.type = "SELECT";
        consume(TokenType.SELECT);
        consume(TokenType.STAR);
        consume(TokenType.FROM);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();

        while (!isEOF()) {
            if (peek().type == TokenType.WHERE) {
                consume(TokenType.WHERE);
                parseWhereClause(q);
            } else if (peek().type == TokenType.ORDER) {
                consume(TokenType.ORDER);
                consume(TokenType.BY);
                q.orderByColumn = consume(TokenType.IDENT).value.toLowerCase();
            } else {
                break;
            }
        }
        return q;
    }

    // INSERT INTO <table> VALUES (v1, v2, ...)
    private Query parseInsert() {
        Query q = new Query();
        q.type = "INSERT";
        consume(TokenType.INSERT);
        consume(TokenType.INTO);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.VALUES);
        consume(TokenType.LPAREN);

        List<String> values = new ArrayList<>();
        while (peek().type != TokenType.RPAREN && !isEOF()) {
            Token val = next();
            values.add(val.value);
            if (peek().type == TokenType.COMMA) consume(TokenType.COMMA);
        }
        consume(TokenType.RPAREN);
        q.values = values;
        return q;
    }

    // UPDATE <table> SET col = val [WHERE ...]
    private Query parseUpdate() {
        Query q = new Query();
        q.type = "UPDATE";
        consume(TokenType.UPDATE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.SET);
        q.setColumn = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.EQUALS);
        q.setValue = next().value;

        if (!isEOF() && peek().type == TokenType.WHERE) {
            consume(TokenType.WHERE);
            parseWhereClause(q);
        }
        return q;
    }

    // DELETE FROM <table> [WHERE ...]
    private Query parseDelete() {
        Query q = new Query();
        q.type = "DELETE";
        consume(TokenType.DELETE);
        consume(TokenType.FROM);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();

        if (!isEOF() && peek().type == TokenType.WHERE) {
            consume(TokenType.WHERE);
            parseWhereClause(q);
        }
        return q;
    }

    // CREATE TABLE <name> (col1 TYPE, col2 TYPE, ...)
    private Query parseCreate() {
        Query q = new Query();
        q.type = "CREATE";
        consume(TokenType.CREATE);
        consume(TokenType.TABLE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.LPAREN);

        List<String> columns = new ArrayList<>();
        while (peek().type != TokenType.RPAREN && !isEOF()) {
            String colName = consume(TokenType.IDENT).value.toLowerCase();
            columns.add(colName);
            if (peek().type == TokenType.IDENT) next(); // skip type (INT, TEXT etc)
            if (peek().type == TokenType.COMMA) consume(TokenType.COMMA);
        }
        consume(TokenType.RPAREN);
        q.columns = columns;
        return q;
    }

    // DROP TABLE <name>
    private Query parseDrop() {
        Query q = new Query();
        q.type = "DROP";
        consume(TokenType.DROP);
        consume(TokenType.TABLE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        return q;
    }

    // ALTER TABLE <name> ADD <col> <type>
    // ALTER TABLE <name> DROP COLUMN <col>
    private Query parseAlter() {
        Query q = new Query();
        q.type = "ALTER";
        consume(TokenType.ALTER);
        consume(TokenType.TABLE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();

        if (peek().type == TokenType.ADD) {
            consume(TokenType.ADD);
            q.alterAction = "ADD";
            q.alterColumn = consume(TokenType.IDENT).value.toLowerCase();
            if (!isEOF() && peek().type == TokenType.IDENT) {
                q.alterType = next().value.toUpperCase();
            }
        } else if (peek().type == TokenType.DROP) {
            consume(TokenType.DROP);
            consume(TokenType.COLUMN);
            q.alterAction = "DROP";
            q.alterColumn = consume(TokenType.IDENT).value.toLowerCase();
        }
        return q;
    }

    // SHOW TABLES
    private Query parseShow() {
        Query q = new Query();
        q.type = "SHOW";
        consume(TokenType.SHOW);
        consume(TokenType.TABLES);
        return q;
    }

    // Parses: col = val [AND col = val ...]
    private void parseWhereClause(Query q) {
        String col = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.EQUALS);
        String val = next().value;
        q.whereColumns.add(col);
        q.whereValues.add(val);

        while (!isEOF() && peek().type == TokenType.AND) {
            consume(TokenType.AND);
            col = consume(TokenType.IDENT).value.toLowerCase();
            consume(TokenType.EQUALS);
            val = next().value;
            q.whereColumns.add(col);
            q.whereValues.add(val);
        }
    }

    // Helper methods
    private Token peek() {
        return tokens.get(pos);
    }

    private Token next() {
        return tokens.get(pos++);
    }

    private Token consume(TokenType expected) {
        Token t = tokens.get(pos);
        if (t.type != expected) {
            throw new RuntimeException("Expected " + expected + " but got " + t.type + " ('" + t.value + "')");
        }
        pos++;
        return t;
    }

    private boolean isEOF() {
        return tokens.get(pos).type == TokenType.EOF;
    }
}
