package com.minidb.minidb.engine;

import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * Manages database transactions including begin, commit, and rollback operations.
 * Handles transaction state and persistence control.
 */
public class TransactionManager {

    private boolean inTransaction = false;
    private IndexManager indexManager;

    public TransactionManager(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    /**
     * Starts a new transaction.
     *
     * @return success message or error if already in transaction
     */
    public String beginTransaction() {
        if (inTransaction) {
            return "Error: Transaction already in progress.";
        }
        inTransaction = true;
        indexManager.setAutoPersistMeta(false);
        return "Transaction started.";
    }

    /**
     * Commits the current transaction.
     *
     * @return success message or error if no active transaction
     */
    public String commitTransaction() {
        if (!inTransaction) {
            return "Error: No active transaction.";
        }
        // Persist all changes here
        inTransaction = false;
        indexManager.setAutoPersistMeta(true);
        indexManager.flushMeta();
        return "Transaction committed.";
    }

    /**
     * Rolls back the current transaction.
     *
     * @return success message or error if no active transaction
     */
    public String rollbackTransaction() {
        if (!inTransaction) {
            return "Error: No active transaction.";
        }
        // Reload from disk to undo changes
        reloadFromDisk();
        inTransaction = false;
        indexManager.setAutoPersistMeta(true);
        return "Transaction rolled back.";
    }

    /**
     * Checks if currently in a transaction.
     *
     * @return true if in transaction, false otherwise
     */
    public boolean isInTransaction() {
        return inTransaction;
    }

    /**
     * Sets the transaction state.
     *
     * @param inTransaction the new transaction state
     */
    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
    }

    // Placeholder for reload logic - would need access to storage
    private void reloadFromDisk() {
        // Implementation would reload schemas, tables, etc. from disk
    }
}