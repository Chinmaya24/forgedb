package com.minidb.minidb.model;

import java.util.List;

public class Query {
    public String type;
    public String tableName;
    public List<String> values;
    public List<String> columns;
    public List<String> selectColumns = new java.util.ArrayList<>();
    public List<String> insertColumns = new java.util.ArrayList<>();
    public List<String> columnTypes = new java.util.ArrayList<>();
    public List<String> autoIncrementColumns = new java.util.ArrayList<>();
    public String setColumn;
    public String setValue;
    public List<String> whereColumns = new java.util.ArrayList<>();
    public List<String> whereValues  = new java.util.ArrayList<>();
    public List<String> whereOps     = new java.util.ArrayList<>();
    public List<String> whereConnectors = new java.util.ArrayList<>();
    public String orderByColumn;
    public String orderByDirection = "ASC";
    public String alterAction;
    public String alterColumn;
    public String alterType;
    public String indexColumn;
    public String joinTable;
    public String joinLeftCol;
    public String joinRightCol;
    public String aggregateFunction;
    public String aggregateColumn;
    public int limit = -1;
    public int offset = 0;
}
