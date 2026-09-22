# Distributed URL Shortener

A scalable and concurrency-aware URL Shortener built with **Java 17, Spring Boot, MySQL, Redis, Maven, and Flyway**.

---

## Today's Progress

Today's work focused on strengthening the project against **concurrent requests** and preparing the system for distributed execution.

### 1. Database Concurrency

Implemented database-level protection against duplicate original URLs.

Added:

```text
original_url_hash
```

to the `url_mapping` table.

The hash is generated using **SHA-256**.

The database now contains a unique constraint:

```text
uk_url_mapping_original_url_hash
```

This ensures that two requests cannot create multiple database records for the same original URL.

### Why this matters

Consider two requests arriving simultaneously:

```text
Request A ──┐
            ├──> Check database
Request B ──┘
```

Both requests could potentially see that the URL does not exist.

The database unique constraint provides the final protection:

```text
Same Original URL
       ↓
Same SHA-256 Hash
       ↓
UNIQUE constraint
       ↓
Only one database record
```

---

## 2. SHA-256 URL Hashing

Added support for generating a deterministic hash of the original URL.

Example:

```text
Original URL
     ↓
SHA-256
     ↓
64-character hexadecimal hash
```

The hash is stored in:

```text
original_url_hash
```

### Why use a hash?

* Same URL produces the same hash.
* SHA-256 produces a fixed 256-bit result.
* The hexadecimal representation is 64 characters.
* It provides a deterministic identifier for duplicate detection.
* It works well with a database UNIQUE constraint.

The original URL is still stored separately.

---

## 3. Flyway V2 Migration

Created:

```text
V2__add_database_concurrency_constraints.sql
```

The migration:

```sql
ALTER TABLE url_mapping
ADD COLUMN original_url_hash VARCHAR(64);

UPDATE url_mapping
SET original_url_hash = SHA2(original_url, 256);

ALTER TABLE url_mapping
MODIFY COLUMN original_url_hash VARCHAR(64) NOT NULL;

ALTER TABLE url_mapping
ADD CONSTRAINT uk_url_mapping_original_url_hash
UNIQUE (original_url_hash);
```

The migration was initially marked as failed by Flyway.

We investigated the failure, confirmed that the database had not been partially modified, repaired the Flyway migration history, and successfully reran the application.

### Current Flyway state

```text
V1 → create url mapping                  → SUCCESS
V2 → add database concurrency constraints → SUCCESS
```

---

## 4. Distributed Concurrency Foundations

Redis is already configured in the project.

Current configuration includes:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379

app.distributed-lock.expiration-seconds=10
```

The project also contains:

```text
concurrency/
└── UrlCreationLock.java
```

`UrlCreationLock` is intended to provide a **distributed lock** for URL creation.

It is different from the database constraint.

### Redis Distributed Lock

Coordinates concurrent requests across multiple application instances:

```text
             Redis
            /     \
           /       \
    Instance 1   Instance 2
           \       /
            \     /
             MySQL
```

### Database Constraint

Provides final database-level correctness:

```text
Application
     ↓
MySQL
     ↓
UNIQUE(original_url_hash)
```

Together:

```text
Redis Lock
    ↓
Coordinate concurrent creation
    ↓
Database
    ↓
UNIQUE constraint
    ↓
Guarantee data integrity
```

`UrlCreationLock` is **not deleted**. It will be completed and properly integrated/tested as part of the distributed concurrency implementation.

---

## 5. Important Concurrency Components

| Component                          | Responsibility                          | Status      |
| ---------------------------------- | --------------------------------------- | ----------- |
| `UrlHashGenerator`                 | Generate deterministic SHA-256 URL hash | Implemented |
| `UrlCreationLock`                  | Redis distributed locking               | In progress |
| `original_url_hash`                | Store URL fingerprint                   | Implemented |
| `uk_url_mapping_original_url_hash` | Prevent duplicate original URLs         | Implemented |
| Flyway V2                          | Database concurrency migration          | Completed   |
| Redis                              | Distributed coordination infrastructure | Configured  |

---

## Current Architecture

```text
                   Client
                     |
                     v
              UrlController
                     |
                     v
                UrlService
                     |
          +----------+----------+
          |                     |
          v                     v
 UrlHashGenerator        UrlCreationLock
          |                     |
          |                   Redis
          |                     |
          +----------+----------+
                     |
                     v
                  MySQL
                     |
          UNIQUE(original_url_hash)
```

---

# Project Technology Stack

* Java 17
* Spring Boot
* Spring MVC
* Spring Data JPA
* Hibernate
* MySQL
* Redis
* Flyway
* Maven
* Bucket4j
* Spring Boot Actuator
* Git / GitHub

---

# Database Schema

Current `url_mapping` table:

```text
url_mapping
├── id
├── short_code
├── original_url
├── original_url_hash
├── created_at
└── expires_at
```

Important constraints:

```text
PRIMARY KEY(id)

UNIQUE(short_code)

UNIQUE(original_url_hash)
```

---

# Concurrency Strategy

The project is being designed with multiple layers of concurrency protection.

### Layer 1 — Application Coordination

Redis distributed lock:

```text
UrlCreationLock
```

### Layer 2 — Database Integrity

MySQL unique constraint:

```text
uk_url_mapping_original_url_hash
```

### Layer 3 — Exception Handling

Database constraint violations are handled by the application so concurrent requests do not result in uncontrolled failures.

---

# Next Step

## Caching — Tomorrow

The next major feature will be **Redis caching**.

Planned flow:

```text
Client
  |
  v
GET /shortCode
  |
  v
Check Redis Cache
  |
  +---- Cache Hit ----> Return Original URL
  |
  +---- Cache Miss
          |
          v
        MySQL
          |
          v
      Store in Redis
          |
          v
      Return URL
```

Topics to cover:

1. Why caching is needed
2. Cache-aside pattern
3. Redis data structures
4. Cache keys and values
5. TTL
6. Cache hit vs cache miss
7. Integrating Spring Cache
8. Redis caching in the URL shortener
9. Cache invalidation
10. Interaction between caching and URL expiration
11. Testing cache behavior
12. Caching considerations in a distributed system

---

## Current Status

### Completed

* URL shortening
* URL redirection
* URL expiration
* Scheduled expiration cleanup
* Rate limiting
* MySQL persistence
* Flyway migrations
* Database concurrency protection
* SHA-256 URL hashing
* Redis configuration
* Distributed concurrency foundation

### In Progress

* Completing and testing `UrlCreationLock`
* Distributed concurrency testing

### Next

* **Redis Caching**

---

## Development Principle

The project is being developed progressively:

```text
Basic URL Shortener
        ↓
Database
        ↓
Concurrency
        ↓
Distributed Concurrency
        ↓
Caching
        ↓
Scalability
        ↓
Production-oriented Distributed System
```

The goal is not just to make the URL shortener work, but to understand **why each distributed-system component is needed and what problem it solves**.
