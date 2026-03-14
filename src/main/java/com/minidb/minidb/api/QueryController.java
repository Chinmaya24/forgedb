package com.minidb.minidb.api;

import com.minidb.minidb.engine.QueryExecutor;
import com.minidb.minidb.model.Query;
import com.minidb.minidb.parser.SQLParser;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/query")
public class QueryController {

    SQLParser parser = new SQLParser();
    QueryExecutor executor = new QueryExecutor();

    @PostMapping
    public String runQuery(@RequestBody String sql) {
        Query query = parser.parse(sql);
        return executor.execute(query);
    }
}