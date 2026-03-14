package com.minidb.minidb.btree;

import java.util.*;

public class BTreeNode {

    public static final int ORDER = 3;

    public List<String> keys;
    public List<List<String>> rows;
    public List<BTreeNode> children;
    public boolean isLeaf;

    public BTreeNode(boolean isLeaf) {
        this.isLeaf   = isLeaf;
        this.keys     = new ArrayList<>();
        this.rows     = new ArrayList<>();
        this.children = new ArrayList<>();
    }
}
