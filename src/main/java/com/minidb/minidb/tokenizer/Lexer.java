package com.minidb.minidb.tokenizer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String input;
    private int pos = 0;

    public Lexer(String input) {
        this.input = input.trim();
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) break;
            char c = input.charAt(pos);
            if (c == '*' )     { tokens.add(new Token(TokenType.STAR,      "*")); pos++; }
            else if (c == '=' ) { tokens.add(new Token(TokenType.EQUALS,    "=")); pos++; }
            else if (c == '!' && peekNext() == '=') { tokens.add(new Token(TokenType.NOT_EQUALS, "!=")); pos += 2; }
            else if (c == '>' && peekNext() == '=') { tokens.add(new Token(TokenType.GTE, ">=")); pos += 2; }
            else if (c == '<' && peekNext() == '=') { tokens.add(new Token(TokenType.LTE, "<=")); pos += 2; }
            else if (c == '>') { tokens.add(new Token(TokenType.GT, ">")); pos++; }
            else if (c == '<') { tokens.add(new Token(TokenType.LT, "<")); pos++; }
            else if (c == ',') { tokens.add(new Token(TokenType.COMMA,     ",")); pos++; }
            else if (c == '(') { tokens.add(new Token(TokenType.LPAREN,    "(")); pos++; }
            else if (c == ')') { tokens.add(new Token(TokenType.RPAREN,    ")")); pos++; }
            else if (c == ';') { tokens.add(new Token(TokenType.SEMICOLON, ";")); pos++; }
            else if (c == '.') { tokens.add(new Token(TokenType.DOT,       ".")); pos++; }
            else if (c == '%') { tokens.add(new Token(TokenType.PERCENT,   "%")); pos++; }
            else if (c == '\'') { tokens.add(readString()); }
            else if (Character.isDigit(c)) { tokens.add(readNumber()); }
            else if (Character.isLetter(c) || c == '_') { tokens.add(readWord()); }
            else { tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c))); pos++; }
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    private char peekNext() {
        if (pos + 1 >= input.length()) return '\0';
        return input.charAt(pos + 1);
    }

    private Token readNumber() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        }
        return new Token(TokenType.NUMBER, input.substring(start, pos));
    }

    private Token readString() {
        pos++;
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '\'') pos++;
        String value = input.substring(start, pos);
        pos++;
        return new Token(TokenType.STRING, value);
    }

    private Token readWord() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) pos++;
        String word = input.substring(start, pos);
        return new Token(classifyWord(word), word);
    }

    private TokenType classifyWord(String word) {
        switch (word.toUpperCase()) {
            case "SELECT":  return TokenType.SELECT;
            case "INSERT":  return TokenType.INSERT;
            case "UPDATE":  return TokenType.UPDATE;
            case "DELETE":  return TokenType.DELETE;
            case "CREATE":  return TokenType.CREATE;
            case "DROP":    return TokenType.DROP;
            case "ALTER":   return TokenType.ALTER;
            case "SHOW":    return TokenType.SHOW;
            case "BEGIN":   return TokenType.BEGIN;
            case "COMMIT":  return TokenType.COMMIT;
            case "ROLLBACK": return TokenType.ROLLBACK;
            case "TRANSACTION": return TokenType.TRANSACTION;
            case "FROM":    return TokenType.FROM;
            case "INTO":    return TokenType.INTO;
            case "VALUES":  return TokenType.VALUES;
            case "SET":     return TokenType.SET;
            case "WHERE":   return TokenType.WHERE;
            case "AND":     return TokenType.AND;
            case "OR":      return TokenType.OR;
            case "ORDER":   return TokenType.ORDER;
            case "BY":      return TokenType.BY;
            case "TABLE":   return TokenType.TABLE;
            case "TABLES":  return TokenType.TABLES;
            case "ADD":     return TokenType.ADD;
            case "COLUMN":  return TokenType.COLUMN;
            case "LIMIT":   return TokenType.LIMIT;
            case "OFFSET":  return TokenType.OFFSET;
            case "INDEX":   return TokenType.INDEX;
            case "INDEXES": return TokenType.INDEXES;
            case "ON":      return TokenType.ON;
            case "JOIN":    return TokenType.JOIN;
            case "INNER":   return TokenType.INNER;
            case "COUNT":   return TokenType.COUNT;
            case "MAX":     return TokenType.MAX;
            case "MIN":     return TokenType.MIN;
            case "SUM":     return TokenType.SUM;
            case "AVG":     return TokenType.AVG;
            case "ASC":     return TokenType.ASC;
            case "DESC":    return TokenType.DESC;
            case "LIKE":    return TokenType.LIKE;
            default:        return TokenType.IDENT;
        }
    }
}
