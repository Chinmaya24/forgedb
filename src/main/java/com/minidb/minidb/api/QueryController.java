package com.minidb.minidb.api;

import com.minidb.minidb.engine.QueryExecutor;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.parser.SQLParser;
import com.minidb.minidb.planner.QueryPlan;
import com.minidb.minidb.planner.QueryPlanner;
import com.minidb.minidb.storage.StorageEngine;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * REST controller for handling SQL query execution in ForgeDB.
 * Provides endpoints for running SQL queries with support for both text and JSON responses.
 */
@RestController
@RequestMapping("/query")
public class QueryController {

    SQLParser parser = new SQLParser();
    QueryExecutor executor = new QueryExecutor();

    /**
     * Executes a SQL query and returns the result.
     * Supports both plain text and JSON response formats based on Accept header or format parameter.
     *
     * @param sql the SQL query string to execute
     * @param accept the Accept header value for content negotiation
     * @param format optional format parameter ("json" for JSON response)
     * @return the query result as a string or QueryResponse object
     */
    @PostMapping(produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Object runQuery(
            @RequestBody String sql,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestParam(value = "format", required = false) String format) {
        long start = System.currentTimeMillis();
        try {
            sql = sql.trim();
            boolean wantsJson = "json".equalsIgnoreCase(format) ||
                (accept != null && accept.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE));

            String result;
            if (sql.toUpperCase().startsWith("EXPLAIN")) {
                String innerSql = sql.substring(7).trim();
                Query query = parser.parse(innerSql);
                Map<String, List<String>> schemas = new HashMap<>();
                Map<String, List<List<String>>> tables = new HashMap<>();
                StorageEngine.load(schemas, tables);
                QueryPlanner planner = new QueryPlanner(schemas, tables);
                QueryPlan plan = planner.plan(query);
                result = plan.toString();
            } else {
                Query query = parser.parse(sql);
                result = executor.execute(query);
            }

            if (!wantsJson) return result;
            return toJsonResponse(result, System.currentTimeMillis() - start);

        } catch (Exception e) {
            String error = "Error: " + e.getMessage() + " | " + e.getClass().getSimpleName();
            boolean wantsJson = "json".equalsIgnoreCase(format) ||
                (accept != null && accept.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE));
            if (!wantsJson) return error;
            QueryResponse response = new QueryResponse();
            response.status = "error";
            response.error = error;
            response.message = error;
            response.executionTimeMs = System.currentTimeMillis() - start;
            response.raw = error;
            return response;
        }
    }

    /**
     * Returns a list of all tables in the database.
     *
     * @return list of table names
     */
    @GetMapping("/tables")
    public List<String> getTables() {
        return new ArrayList<>(executor.getSchemas().keySet());
    }
        QueryResponse response = new QueryResponse();
        response.raw = text;
        response.executionTimeMs = executionMs;
        String trimmed = text == null ? "" : text.trim();

        if (trimmed.startsWith("Error:") || trimmed.startsWith("Invalid")) {
            response.status = "error";
            response.error = trimmed;
            response.message = trimmed;
            return response;
        }

        response.status = "ok";
        response.message = trimmed;
        String[] lines = trimmed.split("\\R");
        if (lines.length > 1 && lines[1].startsWith("---")) {
            for (String header : lines[0].split("\\|")) {
                response.columns.add(header.trim());
            }
            for (int i = 2; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                List<String> row = new ArrayList<>();
                for (String cell : line.split("\\|")) {
                    row.add(cell.trim());
                }
                response.rows.add(row);
            }
            response.rowCount = response.rows.size();
        }
        return response;
    }
}
