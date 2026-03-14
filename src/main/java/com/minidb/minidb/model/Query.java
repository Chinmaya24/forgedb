package com.minidb.minidb.model;

import java.util.List;

public class Query {
    public String type;
    public String tableName;
    public List<String> values;
    public List<String> columns;
    public String setColumn;
    public String setValue;
    public List<String> whereColumns = new java.util.ArrayList<>();
    public List<String> whereValues  = new java.util.ArrayList<>();
    public String orderByColumn;
    public String alterAction;
    public String alterColumn;
    public String alterType;
    public String indexColumn;
    public String joinTable;
    public String joinLeftCol;
    public String joinRightCol;
    public String aggregateFunction;
    public String aggregateColumn;
}
