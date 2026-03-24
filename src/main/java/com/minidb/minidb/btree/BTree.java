package com.minidb.minidb.btree;

import java.util.*;

public class BTree {

    private BTreeNode root;
    private final int ORDER = BTreeNode.ORDER;

    public BTree() {
        root = new BTreeNode(true);
    }

    // Insert a row with a given key
    public void insert(String key, List<String> row) {
        BTreeNode r = root;
        if (r.keys.size() == 2 * ORDER - 1) {
            BTreeNode newRoot = new BTreeNode(false);
            newRoot.children.add(root);
            splitChild(newRoot, 0);
            root = newRoot;
        }
        insertNonFull(root, key, row);
    }

    private void insertNonFull(BTreeNode node, String key, List<String> row) {
        int i = node.keys.size() - 1;
        if (node.isLeaf) {
            node.keys.add(null);
            node.rows.add(null);
            while (i >= 0 && compareKeys(key, node.keys.get(i)) < 0) {
                node.keys.set(i + 1, node.keys.get(i));
                node.rows.set(i + 1, node.rows.get(i));
                i--;
            }
            node.keys.set(i + 1, key);
            node.rows.set(i + 1, row);
        } else {
            while (i >= 0 && compareKeys(key, node.keys.get(i)) < 0) i--;
            i++;
            if (node.children.get(i).keys.size() == 2 * ORDER - 1) {
                splitChild(node, i);
                if (compareKeys(key, node.keys.get(i)) > 0) i++;
            }
            insertNonFull(node.children.get(i), key, row);
        }
    }

    private void splitChild(BTreeNode parent, int i) {
        BTreeNode full  = parent.children.get(i);
        BTreeNode right = new BTreeNode(full.isLeaf);
        int mid = ORDER - 1;

        parent.keys.add(i, full.keys.get(mid));
        parent.rows.add(i, full.rows.get(mid));
        parent.children.add(i + 1, right);

        right.keys.addAll(new ArrayList<>(full.keys.subList(mid + 1, full.keys.size())));
        right.rows.addAll(new ArrayList<>(full.rows.subList(mid + 1, full.rows.size())));

        full.keys  = new ArrayList<>(full.keys.subList(0, mid));
        full.rows  = new ArrayList<>(full.rows.subList(0, mid));

        if (!full.isLeaf) {
            right.children.addAll(new ArrayList<>(full.children.subList(mid + 1, full.children.size())));
            full.children  = new ArrayList<>(full.children.subList(0, mid + 1));
        }
    }

    // Search for a row by exact key
    public List<String> search(String key) {
        return searchNode(root, key);
    }

    private List<String> searchNode(BTreeNode node, String key) {
        int i = 0;
        while (i < node.keys.size() && compareKeys(key, node.keys.get(i)) > 0) i++;
        if (i < node.keys.size() && key.equals(node.keys.get(i))) {
            return node.rows.get(i);
        }
        if (node.isLeaf) return null;
        return searchNode(node.children.get(i), key);
    }

    // Get all rows in sorted order
    public List<List<String>> getAllRows() {
        List<List<String>> result = new ArrayList<>();
        collectRows(root, result);
        return result;
    }

    private void collectRows(BTreeNode node, List<List<String>> result) {
        for (int i = 0; i < node.keys.size(); i++) {
            if (!node.isLeaf) collectRows(node.children.get(i), result);
            result.add(node.rows.get(i));
        }
        if (!node.isLeaf) collectRows(node.children.get(node.children.size() - 1), result);
    }

    // Update a row by key
    public boolean update(String key, int colIdx, String newValue) {
        return updateNode(root, key, colIdx, newValue);
    }

    private boolean updateNode(BTreeNode node, String key, int colIdx, String newValue) {
        int i = 0;
        while (i < node.keys.size() && compareKeys(key, node.keys.get(i)) > 0) i++;
        if (i < node.keys.size() && key.equals(node.keys.get(i))) {
            node.rows.get(i).set(colIdx, newValue);
            return true;
        }
        if (node.isLeaf) return false;
        return updateNode(node.children.get(i), key, colIdx, newValue);
    }

    // Delete a row by key
    public boolean delete(String key) {
        return deleteFromNode(root, key);
    }

    private boolean deleteFromNode(BTreeNode node, String key) {
        int i = 0;
        while (i < node.keys.size() && compareKeys(key, node.keys.get(i)) > 0) i++;
        if (i < node.keys.size() && key.equals(node.keys.get(i))) {
            node.keys.remove(i);
            node.rows.remove(i);
            return true;
        }
        if (node.isLeaf) return false;
        return deleteFromNode(node.children.get(i), key);
    }

    // Compare keys - numeric if possible, else string
    private int compareKeys(String a, String b) {
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        } catch (NumberFormatException e) {
            return a.compareToIgnoreCase(b);
        }
    }

    public BTreeNode getRoot() { return root; }
    public void setRoot(BTreeNode root) { this.root = root; }
}
