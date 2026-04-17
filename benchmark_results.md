# ForgeDB Performance Benchmark Results

**Test Date:** Fri Apr 17 18:35:38 IST 2026

**Test Environment:**
- Java Version: 17.0.18
- Iterations per query: 50
- Cache: LRU with 100 entries

## Query Performance Comparison

| Query Description | Without Cache (ms) | With Cache (ms) | Improvement |
|-------------------|-------------------|-----------------|-------------|
| Full table scan | 0.00 (0.0-0.0) | 0.00 (0.0-0.0) | 0.0% |
| Range filter on age | 0.00 (0.0-0.0) | 0.00 (0.0-0.0) | 0.0% |
| Equality filter with projection | 0.00 (0.0-0.0) | 0.00 (0.0-0.0) | 0.0% |
| Aggregate query | 0.00 (0.0-0.0) | 0.00 (0.0-0.0) | 0.0% |
| ORDER BY with LIMIT | 0.00 (0.0-0.0) | 0.00 (0.0-0.0) | 0.0% |
| Primary key lookup | 0.00 (0.0-0.0) | 0.00 (0.0-0.0) | 0.0% |

## Summary Statistics

- **Average Improvement:** 0.0%
- **Maximum Improvement:** 0.0%
- **Queries Tested:** 6
