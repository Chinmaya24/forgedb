# ForgeDB (MiniDB)

ForgeDB is a lightweight SQL-like database engine built with Java and Spring Boot.  
It includes:

- A REST API to execute queries
- A browser UI (`index.html`) for interactive query testing
- Basic storage persistence to disk (`data/`)
- B-Tree-based primary key lookups
- Secondary index support
- A simple query planner with `EXPLAIN`
- Atomic persistence writes for core storage files

## Tech Stack

- Java 17
- Spring Boot 3
- Gradle

## Project Structure

- `src/main/java/com/minidb/minidb` - application source code
- `src/main/java/com/minidb/minidb/api` - REST controllers
- `src/main/java/com/minidb/minidb/parser` - SQL parser and lexer
- `src/main/java/com/minidb/minidb/engine` - query execution and index handling
- `src/main/java/com/minidb/minidb/btree` - B-Tree implementation
- `src/main/java/com/minidb/minidb/storage` - persistence layer
- `src/main/resources/static/index.html` - web UI

## Getting Started

### Prerequisites

- JDK 17+

### Run the app

On Windows:

```powershell
.\gradlew.bat bootRun
```

On macOS/Linux:

```bash
./gradlew bootRun
```

The server starts on:

- `http://localhost:8080`

Health endpoint:

- `GET /hello` -> `MiniDB running`

## Query API

Execute SQL by calling:

- `POST /query`
- `Content-Type: text/plain`
- Body: raw SQL string
- Response modes:
  - Default legacy text/plain
  - JSON mode with `?format=json` or `Accept: application/json`

Example:

```bash
curl -X POST http://localhost:8080/query \
  -H "Content-Type: text/plain" \
  -d "SHOW TABLES"
```

JSON mode example:

```bash
curl -X POST "http://localhost:8080/query?format=json" \
  -H "Content-Type: text/plain" \
  -H "Accept: application/json" \
  -d "SELECT id, name FROM users WHERE age >= 18"
```

## Supported Query Types

- `SHOW TABLES`
- `CREATE TABLE`
- `DROP TABLE`
- `ALTER TABLE ... ADD ...`
- `ALTER TABLE ... DROP COLUMN ...`
- `INSERT INTO ... VALUES (...)`
- `INSERT INTO table(col1, col2) VALUES (...)`
- `SELECT * FROM ...`
- `SELECT col1, col2 FROM ...`
- `SELECT ... WHERE ...` (supports `=`, `!=`, `>`, `<`, `>=`, `<=`, `LIKE`, with `AND`/`OR`)
- `ORDER BY ... ASC|DESC`
- `LIMIT ... OFFSET ...`
- `UPDATE ... SET ... WHERE ...`
- `DELETE FROM ... WHERE ...`
- Aggregates: `COUNT`, `MAX`, `MIN`, `SUM`, `AVG`
- Join: `INNER JOIN` / `JOIN`
- Index commands:
  - `CREATE INDEX ON table(column)`
  - `DROP INDEX ON table(column)`
  - `SHOW INDEXES ON table`
- `EXPLAIN <query>`
- Transaction commands:
  - `BEGIN` or `BEGIN TRANSACTION`
  - `COMMIT`
  - `ROLLBACK`

## MySQL-style CREATE TABLE Compatibility

ForgeDB now accepts common MySQL-style column definitions for easier onboarding, for example:

```sql
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
```

Behavior notes:

- `VARCHAR(n)`, `TIMESTAMP`, `DATE`, `DATETIME` are currently mapped to internal `TEXT`.
- `PRIMARY KEY`, `NOT NULL`, `UNIQUE`, and `DEFAULT ...` are accepted by the parser.
- `AUTO_INCREMENT` is supported for inserts when the auto-increment column value is omitted.

## Example SQL Session

```sql
CREATE TABLE users (id INT, name TEXT, age INT);
INSERT INTO users VALUES (1, 'Alice', 24);
INSERT INTO users VALUES (2, 'Bob', 31);
SELECT * FROM users;
SELECT * FROM users WHERE age = 24;
SELECT * FROM users ORDER BY age DESC LIMIT 1;
SELECT COUNT(*) FROM users;
CREATE INDEX ON users(name);
SHOW INDEXES ON users;
EXPLAIN SELECT * FROM users WHERE name = 'Alice';
```

## Web UI

Open:

- `http://localhost:8080`

The UI includes:

- SQL editor with `Ctrl+Enter` execution
- Quick query templates
- Sidebar for tables and indexes
- Result rendering for tables, aggregates, and status messages

## Persistence

Data is stored locally in the `data/` directory:

- `data/schemas.json`
- `data/tables.json`
- `data/schema_types.json`
- `data/indexes/meta.json`

B-Tree data is also persisted by the storage layer.
All core persistence writes now use an atomic strategy (`.tmp` write + move) to reduce corruption risk.

## Query Planning Notes

- `EXPLAIN` reports estimated scan strategy.
- Single-condition equality filters are marked as `INDEX_SCAN` candidates.
- Other filters default to `FULL_TABLE_SCAN`.
- Executor uses index lookups for equality filters on indexed columns and returns all matching rows.

## Run Tests

```powershell
.\gradlew.bat test
```

If your local `build/` folder is locked by another process, run:

```powershell
.\gradlew.bat --% -Dorg.gradle.project.buildDir=build_tmp test
```

## CI

A GitHub Actions workflow is included at `.github/workflows/ci.yml` and runs the test suite on pushes and pull requests.

## Notes

- This project is intended as an educational mini database engine.
- SQL support is intentionally simplified and does not aim for full SQL compatibility.
