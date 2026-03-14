package com.minidb.minidb.model;

import java.util.List;

public class Query {
    public String type;
    public String tableName;
    public List<String> values;
    public List<String> columns;
    public String whereColumn;
    public String whereValue;
    public String setColumn;
    public String setValue;
}
