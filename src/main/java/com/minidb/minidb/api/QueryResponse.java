package com.minidb.minidb.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object for SQL query execution results.
 * Contains status, execution time, and parsed tabular data.
 */
public class QueryResponse {
    /** The status of the query execution ("ok" or "error") */
    public String status;
    /** Human-readable message describing the result */
    public String message;
    /** Error message if status is "error" */
    public String error;
    /** Execution time in milliseconds */
    public long executionTimeMs;
    /** Number of rows returned (for SELECT queries) */
    public int rowCount;
    /** Column names for tabular results */
    public List<String> columns = new ArrayList<>();
    /** Row data as list of string arrays */
    public List<List<String>> rows = new ArrayList<>();
    /** Raw text result */
    public String raw;
}
