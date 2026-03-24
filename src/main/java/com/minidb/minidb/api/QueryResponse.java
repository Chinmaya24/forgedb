package com.minidb.minidb.api;

import java.util.ArrayList;
import java.util.List;

public class QueryResponse {
    public String status;
    public String message;
    public String error;
    public long executionTimeMs;
    public int rowCount;
    public List<String> columns = new ArrayList<>();
    public List<List<String>> rows = new ArrayList<>();
    public String raw;
}
