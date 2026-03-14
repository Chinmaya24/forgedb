package com.minidb.minidb.planner;

public class QueryPlan {

    public enum ScanType {
        FULL_TABLE_SCAN,
        INDEX_SCAN,        // future use
        NO_SCAN            // for CREATE, DROP, ALTER, SHOW
    }

    public String queryType;
    public String tableName;
    public ScanType scanType;
    public int estimatedRows;
    public String planDescription;
    public boolean isValid;
    public String errorMessage;

    public static QueryPlan invalid(String error) {
        QueryPlan p = new QueryPlan();
        p.isValid = false;
        p.errorMessage = error;
        return p;
    }

    public static QueryPlan valid(String queryType, String tableName, ScanType scanType, int estimatedRows, String description) {
        QueryPlan p = new QueryPlan();
        p.isValid = true;
        p.queryType = queryType;
        p.tableName = tableName;
        p.scanType = scanType;
        p.estimatedRows = estimatedRows;
        p.planDescription = description;
        return p;
    }

    @Override
    public String toString() {
        if (!isValid) return "Invalid plan: " + errorMessage;
        return "[" + queryType + "] table=" + tableName +
               " scan=" + scanType +
               " estimatedRows=" + estimatedRows +
               " | " + planDescription;
    }
}
