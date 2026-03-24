package com.minidb.minidb.planner;

import com.minidb.minidb.model.Query;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryPlannerTests {

    @Test
    void marksSingleEqualitySelectAsIndexCandidate() {
        Map<String, List<String>> schemas = new HashMap<>();
        Map<String, List<List<String>>> tables = new HashMap<>();
        schemas.put("users", List.of("id", "name"));
        tables.put("users", List.of(List.of("1", "alice"), List.of("2", "bob")));
        QueryPlanner planner = new QueryPlanner(schemas, tables);

        Query q = new Query();
        q.type = "SELECT";
        q.tableName = "users";
        q.whereColumns.add("name");
        q.whereOps.add("=");
        q.whereValues.add("alice");

        QueryPlan plan = planner.plan(q);
        assertEquals(QueryPlan.ScanType.INDEX_SCAN, plan.scanType);
    }

    @Test
    void keepsRangeSelectAsFullScan() {
        Map<String, List<String>> schemas = new HashMap<>();
        Map<String, List<List<String>>> tables = new HashMap<>();
        schemas.put("users", List.of("id", "age"));
        tables.put("users", List.of(List.of("1", "20"), List.of("2", "30")));
        QueryPlanner planner = new QueryPlanner(schemas, tables);

        Query q = new Query();
        q.type = "SELECT";
        q.tableName = "users";
        q.whereColumns.add("age");
        q.whereOps.add(">");
        q.whereValues.add("21");

        QueryPlan plan = planner.plan(q);
        assertEquals(QueryPlan.ScanType.FULL_TABLE_SCAN, plan.scanType);
    }
}
