package com.minidb.minidb.api;

import com.minidb.minidb.engine.QueryExecutor;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.parser.SQLParser;
import com.minidb.minidb.planner.QueryPlan;
import com.minidb.minidb.planner.QueryPlanner;
import com.minidb.minidb.storage.StorageEngine;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/query")
public class QueryController {

    SQLParser parser = new SQLParser();
    QueryExecutor executor = new QueryExecutor();

    @PostMapping
    public String runQuery(@RequestBody String sql) {
        try {
            sql = sql.trim();

            if (sql.toUpperCase().startsWith("EXPLAIN")) {
                String innerSql = sql.substring(7).trim();
                Query query = parser.parse(innerSql);
                Map<String, List<String>> schemas = new HashMap<>();
                Map<String, List<List<String>>> tables = new HashMap<>();
                StorageEngine.load(schemas, tables);
                QueryPlanner planner = new QueryPlanner(schemas, tables);
                QueryPlan plan = planner.plan(query);
                return plan.toString();
            }

            Query query = parser.parse(sql);
            return executor.execute(query);

        } catch (Exception e) {
            return "Error: " + e.getMessage() + " | " + e.getClass().getSimpleName();
        }
    }
}
