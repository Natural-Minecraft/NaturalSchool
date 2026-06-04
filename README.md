# NaturalSchool

NaturalSchool is a high-performance core plugin for Paper servers (targeting Java Edition 1.21.1 and Java 17+) built with a Core-Infrastructure architectural pattern. It manages player academic student profiles smoothly with direct SQLite support and a MySQL connection pool powered by HikariCP.

## Key Features

- **Core-Infrastructure Pattern**: Decoupled modules. Configuration and Database act as global core components, while student player data runs under a separate caching manager module.
- **High-Performance Database Support**:
  - **SQLite**: Local database storage using `school.db` located inside the plugin's data folder.
  - **MySQL**: Connection pool initialization utilizing HikariCP to prevent connection leaks, connection timeouts, and optimized prepared statement caching.
- **Anti-Memory Leak Guardrails**:
  - **No Bukkit Player Objects Cached**: The cache strictly keys records using `java.util.UUID`.
  - **Thread-Safe Runtime Cache**: Online players' database records are stored in a managed `ConcurrentHashMap<UUID, StudentProfile>`.
  - **Asynchronous Join Processing**: Fetches profiles asynchronously from the database during the join lifecycle event.
  - **Strict Cache Eviction**: Saves profiles asynchronously and strictly clears the cache entry immediately on quit or kick events.
  - **No Ticking Drops**: All database (I/O) queries run on the asynchronous Bukkit Scheduler thread to maintain 20 TPS.

## Configuration (`config.yml`)

The plugin can be configured with MySQL server settings, pool limits, and default start parameters for new players.

```yaml
database:
  storage-type: "SQLITE" # SQLITE or MYSQL
  host: "localhost"
  port: 3306
  database: "naturalschool"
  username: "root"
  password: "password"
  pool-settings:
    maximum-pool-size: 10
    connection-timeout: 30000

academic-settings:
  general-settings:
    enable-debug-logs: false
  default-start-class: 1
  default-start-stage: "SD"
```

## Compilation & Installation

1. **Requirements**: Java 17+ and Apache Maven installed.
2. **Build package**:
   ```bash
   mvn clean package
   ```
3. Copy the compiled JAR `NaturalSchool-1.0.1.jar` from the `target` folder into your Paper server's `plugins` folder.
