package com.minidb.minidb.tokenizer;

public enum TokenType {

    // Keywords
    SELECT, INSERT, UPDATE, DELETE,
    CREATE, DROP, ALTER, SHOW,
    FROM, INTO, VALUES, SET,
    WHERE, AND, OR, ORDER, BY,
    TABLE, TABLES, ADD, COLUMN,
    LIMIT,

    // Symbols
    STAR,       // *
    EQUALS,     // =
    COMMA,      // ,
    LPAREN,     // (
    RPAREN,     // )
    SEMICOLON,  // ;

    // Literals
    IDENT,      // table names, column names
    NUMBER,     // 123
    STRING,     // 'hello'

    // Special
    EOF,
    UNKNOWN
}
