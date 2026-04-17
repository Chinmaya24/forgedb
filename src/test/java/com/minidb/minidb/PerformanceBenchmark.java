package com.minidb.minidb;

import com.minidb.minidb.api.QueryExecutor;

public class PerformanceBenchmark {
    public static void main(String[] args) {
        QueryExecutor executor = new QueryExecutor();

        String query = "SELECT * FROM users WHERE age > 25";

        // Run without cache
        long startNoCache = System.currentTimeMillis();
        executor.execute(query, false); // false disables cache
        long endNoCache = System.currentTimeMillis();

        // Run with cache
        long startCache = System.currentTimeMillis();
        executor.execute(query, true); // true enables cache
        long endCache = System.currentTimeMillis();

        System.out.println("Without Cache: " + (endNoCache - startNoCache) + " ms");
        System.out.println("With Cache: " + (endCache - startCache) + " ms");
    }
}
