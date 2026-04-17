package com.minidb.minidb.parser;

import java.util.ArrayList;
import java.util.List;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.tokenizer.Lexer;
import com.minidb.minidb.tokenizer.Token;
import com.minidb.minidb.tokenizer.TokenType;

/**
 * SQL parser that converts SQL strings into Query objects.
 * Supports SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, SHOW, and transaction commands.
 */
public class SQLParser {

    private List<Token> tokens;
    private int pos;

    /**
     * Parses a SQL string into a Query object.
     *
     * @param sql the SQL query string to parse
     * @return the parsed Query object
     * @throws RuntimeException if the SQL syntax is invalid
     */
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
            case BEGIN:  return parseBegin();
            case COMMIT: return parseCommit();
            case ROLLBACK: return parseRollback();
            default: q.type = "UNKNOWN"; return q;
        }
    }

    private Query parseBegin() {
        Query q = new Query();
        consume(TokenType.BEGIN);
        if (!isEOF() && peek().type == TokenType.TRANSACTION) consume(TokenType.TRANSACTION);
        q.type = "BEGIN";
        return q;
    }

    private Query parseCommit() {
        Query q = new Query();
        consume(TokenType.COMMIT);
        q.type = "COMMIT";
        return q;
    }

    private Query parseRollback() {
        Query q = new Query();
        consume(TokenType.ROLLBACK);
        q.type = "ROLLBACK";
        return q;
    }

    private Query parseSelect() {
        Query q = new Query();
        consume(TokenType.SELECT);

        if (isAggregateToken(peek().type)) {
            q.type = "AGGREGATE";
            q.aggregateFunction = next().value.toUpperCase();
            consume(TokenType.LPAREN);
            if (peek().type == TokenType.STAR) { consume(TokenType.STAR); q.aggregateColumn = "*"; }
            else { q.aggregateColumn = consume(TokenType.IDENT).value.toLowerCase(); }
            consume(TokenType.RPAREN);
            consume(TokenType.FROM);
            q.tableName = consume(TokenType.IDENT).value.toLowerCase();
            if (!isEOF() && peek().type == TokenType.WHERE) { consume(TokenType.WHERE); parseWhereClause(q); }
            return q;
        }

        q.type = "SELECT";
        if (peek().type == TokenType.STAR) {
            consume(TokenType.STAR);
        } else {
            while (!isEOF()) {
                q.selectColumns.add(consume(TokenType.IDENT).value.toLowerCase());
                if (peek().type == TokenType.COMMA) consume(TokenType.COMMA);
                else break;
            }
        }
        consume(TokenType.FROM);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();

        while (!isEOF()) {
            if (peek().type == TokenType.JOIN || peek().type == TokenType.INNER) {
                if (peek().type == TokenType.INNER) consume(TokenType.INNER);
                consume(TokenType.JOIN);
                q.joinTable = consume(TokenType.IDENT).value.toLowerCase();
                consume(TokenType.ON);
                String lt = consume(TokenType.IDENT).value.toLowerCase();
                consume(TokenType.DOT);
                q.joinLeftCol = lt + "." + consume(TokenType.IDENT).value.toLowerCase();
                consume(TokenType.EQUALS);
                String rt = consume(TokenType.IDENT).value.toLowerCase();
                consume(TokenType.DOT);
                q.joinRightCol = rt + "." + consume(TokenType.IDENT).value.toLowerCase();
            } else if (peek().type == TokenType.WHERE) {
                consume(TokenType.WHERE);
                parseWhereClause(q);
            } else if (peek().type == TokenType.ORDER) {
                consume(TokenType.ORDER);
                consume(TokenType.BY);
                q.orderByColumn = consume(TokenType.IDENT).value.toLowerCase();
                if (!isEOF() && peek().type == TokenType.DESC) { consume(TokenType.DESC); q.orderByDirection = "DESC"; }
                else if (!isEOF() && peek().type == TokenType.ASC) { consume(TokenType.ASC); q.orderByDirection = "ASC"; }
            } else if (peek().type == TokenType.LIMIT) {
                consume(TokenType.LIMIT);
                q.limit = Integer.parseInt(consume(TokenType.NUMBER).value);
                if (!isEOF() && peek().type == TokenType.OFFSET) {
                    consume(TokenType.OFFSET);
                    q.offset = Integer.parseInt(consume(TokenType.NUMBER).value);
                }
            } else break;
        }
        return q;
    }

    private Query parseInsert() {
        Query q = new Query();
        q.type = "INSERT";
        consume(TokenType.INSERT);
        consume(TokenType.INTO);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        if (peek().type == TokenType.LPAREN) {
            consume(TokenType.LPAREN);
            while (peek().type != TokenType.RPAREN && !isEOF()) {
                q.insertColumns.add(consume(TokenType.IDENT).value.toLowerCase());
                if (peek().type == TokenType.COMMA) consume(TokenType.COMMA);
            }
            consume(TokenType.RPAREN);
        }
        consume(TokenType.VALUES);
        consume(TokenType.LPAREN);
        List<String> values = new ArrayList<>();
        while (peek().type != TokenType.RPAREN && !isEOF()) {
            values.add(next().value);
            if (peek().type == TokenType.COMMA) consume(TokenType.COMMA);
        }
        consume(TokenType.RPAREN);
        q.values = values;
        return q;
    }

    private Query parseUpdate() {
        Query q = new Query();
        q.type = "UPDATE";
        consume(TokenType.UPDATE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.SET);
        q.setColumn = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.EQUALS);
        q.setValue = next().value;
        if (!isEOF() && peek().type == TokenType.WHERE) { consume(TokenType.WHERE); parseWhereClause(q); }
        return q;
    }

    private Query parseDelete() {
        Query q = new Query();
        q.type = "DELETE";
        consume(TokenType.DELETE);
        consume(TokenType.FROM);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        if (!isEOF() && peek().type == TokenType.WHERE) { consume(TokenType.WHERE); parseWhereClause(q); }
        return q;
    }

    private Query parseCreate() {
        Query q = new Query();
        consume(TokenType.CREATE);
        if (peek().type == TokenType.INDEX) {
            consume(TokenType.INDEX); consume(TokenType.ON);
            if (peek().type != TokenType.IDENT) {
                throw new RuntimeException("Invalid CREATE INDEX syntax. Expected table name after ON. Example: CREATE INDEX ON users(name)");
            }
            q.type = "CREATE_INDEX";
            q.tableName = consume(TokenType.IDENT).value.toLowerCase();
            consume(TokenType.LPAREN);
            if (peek().type != TokenType.IDENT) {
                throw new RuntimeException("Invalid CREATE INDEX syntax. Expected column name inside parentheses. Example: CREATE INDEX ON users(name)");
            }
            q.indexColumn = consume(TokenType.IDENT).value.toLowerCase();
            consume(TokenType.RPAREN);
            return q;
        }
        q.type = "CREATE";
        consume(TokenType.TABLE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        consume(TokenType.LPAREN);
        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<String> autoIncrementColumns = new ArrayList<>();
        while (peek().type != TokenType.RPAREN && !isEOF()) {
            String columnName = consume(TokenType.IDENT).value.toLowerCase();
            columns.add(columnName);
            String rawType = "TEXT";
            if (peek().type == TokenType.IDENT) {
                rawType = next().value.toUpperCase();
                // Consume optional type params like VARCHAR(50), DECIMAL(10,2).
                if (peek().type == TokenType.LPAREN) {
                    int depth = 0;
                    do {
                        if (peek().type == TokenType.LPAREN) depth++;
                        if (peek().type == TokenType.RPAREN) depth--;
                        next();
                    } while (depth > 0 && !isEOF());
                }
            }

            // Consume optional column constraints (PRIMARY KEY, NOT NULL, UNIQUE, AUTO_INCREMENT, DEFAULT ...).
            while (peek().type != TokenType.COMMA &&
                   peek().type != TokenType.RPAREN &&
                   !isEOF()) {
                String tokenValue = next().value.toUpperCase();
                if ("AUTO_INCREMENT".equals(tokenValue)) {
                    autoIncrementColumns.add(columnName);
                }
            }

            types.add(normalizeSqlType(rawType));
            if (peek().type == TokenType.COMMA) consume(TokenType.COMMA);
        }
        consume(TokenType.RPAREN);
        q.columns = columns;
        q.columnTypes = types;
        q.autoIncrementColumns = autoIncrementColumns;
        return q;
    }

    private Query parseDrop() {
        Query q = new Query();
        consume(TokenType.DROP);
        if (peek().type == TokenType.INDEX) {
            consume(TokenType.INDEX); consume(TokenType.ON);
            q.type = "DROP_INDEX";
            q.tableName = consume(TokenType.IDENT).value.toLowerCase();
            consume(TokenType.LPAREN);
            q.indexColumn = consume(TokenType.IDENT).value.toLowerCase();
            consume(TokenType.RPAREN);
            return q;
        }
        q.type = "DROP";
        consume(TokenType.TABLE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        return q;
    }

    private Query parseAlter() {
        Query q = new Query();
        q.type = "ALTER";
        consume(TokenType.ALTER); consume(TokenType.TABLE);
        q.tableName = consume(TokenType.IDENT).value.toLowerCase();
        if (peek().type == TokenType.ADD) {
            consume(TokenType.ADD); q.alterAction = "ADD";
            q.alterColumn = consume(TokenType.IDENT).value.toLowerCase();
            if (!isEOF() && peek().type == TokenType.IDENT) q.alterType = next().value.toUpperCase();
        } else if (peek().type == TokenType.DROP) {
            consume(TokenType.DROP); consume(TokenType.COLUMN);
            q.alterAction = "DROP";
            q.alterColumn = consume(TokenType.IDENT).value.toLowerCase();
        }
        return q;
    }

    private Query parseShow() {
        Query q = new Query();
        consume(TokenType.SHOW);
        if (!isEOF() && peek().type == TokenType.INDEXES) {
            consume(TokenType.INDEXES); consume(TokenType.ON);
            q.type = "SHOW_INDEXES";
            q.tableName = consume(TokenType.IDENT).value.toLowerCase();
            return q;
        }
        q.type = "SHOW";
        consume(TokenType.TABLES);
        return q;
    }

    // Parses WHERE with AND/OR and LIKE/=
    private void parseWhereClause(Query q) {
        parseCondition(q);
        while (!isEOF() && (peek().type == TokenType.AND || peek().type == TokenType.OR)) {
            String connector = next().value.toUpperCase();
            q.whereConnectors.add(connector);
            parseCondition(q);
        }
    }

    private void parseCondition(Query q) {
        String col = consume(TokenType.IDENT).value.toLowerCase();
        String op;
        if (peek().type == TokenType.LIKE) { consume(TokenType.LIKE); op = "LIKE"; }
        else if (peek().type == TokenType.EQUALS) { consume(TokenType.EQUALS); op = "="; }
        else if (peek().type == TokenType.NOT_EQUALS) { consume(TokenType.NOT_EQUALS); op = "!="; }
        else if (peek().type == TokenType.GT) { consume(TokenType.GT); op = ">"; }
        else if (peek().type == TokenType.LT) { consume(TokenType.LT); op = "<"; }
        else if (peek().type == TokenType.GTE) { consume(TokenType.GTE); op = ">="; }
        else if (peek().type == TokenType.LTE) { consume(TokenType.LTE); op = "<="; }
        else throw new RuntimeException("Expected condition operator after column '" + col + "'");
        String val = readValue();
        q.whereColumns.add(col);
        q.whereOps.add(op);
        q.whereValues.add(val);
    }

    // Read value - handles identifiers, numbers, strings, and % wildcard
    private String readValue() {
        StringBuilder sb = new StringBuilder();
        while (!isEOF() &&
               peek().type != TokenType.AND &&
               peek().type != TokenType.OR &&
               peek().type != TokenType.ORDER &&
               peek().type != TokenType.LIMIT &&
               peek().type != TokenType.OFFSET &&
               peek().type != TokenType.EOF) {
            sb.append(next().value);
        }
        return sb.toString().trim();
    }

    private boolean isAggregateToken(TokenType t) {
        return t == TokenType.COUNT || t == TokenType.MAX || t == TokenType.MIN || t == TokenType.SUM || t == TokenType.AVG;
    }

    private String normalizeSqlType(String rawType) {
        String t = rawType.toUpperCase();
        if (t.equals("INT") || t.equals("INTEGER")) return "INT";
        if (t.equals("BIGINT") || t.equals("LONG")) return "LONG";
        if (t.equals("FLOAT")) return "FLOAT";
        if (t.equals("DOUBLE") || t.equals("DECIMAL")) return "DOUBLE";
        // Map richer SQL types into the current engine's TEXT capability.
        if (t.equals("VARCHAR") || t.equals("CHAR") || t.equals("TEXT") || t.equals("TIMESTAMP") || t.equals("DATE") || t.equals("DATETIME")) return "TEXT";
        return "TEXT";
    }

    private Token peek() { return tokens.get(pos); }
    private Token next() { return tokens.get(pos++); }
    private Token consume(TokenType expected) {
        Token t = tokens.get(pos);
        if (t.type != expected) throw new RuntimeException("Expected " + expected + " but got " + t.type + " ('" + t.value + "')");
        pos++;
        return t;
    }
    private boolean isEOF() { return tokens.get(pos).type == TokenType.EOF; }
}
