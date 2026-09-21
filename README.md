# Distributed URL Shortener

A backend URL-shortening service built with **Java 17, Spring Boot, Maven, and MySQL**, designed with concurrency, thread safety, expiration, and future distributed-system scalability in mind.

The project is being developed step-by-step to understand how a production-style URL shortener handles **concurrent requests, duplicate data, database constraints, expiration, caching, rate limiting, and horizontal scaling**.

---

## Tech Stack

* **Java 17**
* **Spring Boot**
* **Maven**
* **Spring Data JPA / Hibernate**
* **MySQL**
* **Flyway**
* **REST API**
* **Git & GitHub**

---

## Current Features

### 1. URL Shortening

Clients can submit an original URL through:

```http
POST /shorten
```

The service generates a short code and stores the mapping in MySQL.

Basic flow:

```text
POST /shorten
      ↓
UrlController
      ↓
UrlService
      ↓
UrlRepository
      ↓
MySQL
```

---

### 2. Short URL Redirection

A short code can be used to retrieve the original URL.

```http
GET /{shortCode}
```

The service:

1. Searches for the short code.
2. Checks whether the mapping exists.
3. Checks whether the URL has expired.
4. Returns the original URL when valid.

---

### 3. URL Expiration

Each generated short URL has a **4-hour expiration time**.

The expiration timestamp is stored in:

```text
expires_at
```

The expiration time is calculated when the URL is created:

```java
LocalDateTime.now().plusHours(expirationHours)
```

The expiration period is configurable through:

```properties
app.url-expiration-hours=4
```

---

### 4. Automatic Expiration Cleanup

The project contains:

```text
UrlExpirationScheduler
```

The scheduler periodically removes expired URL mappings from the database.

Important distinction:

```text
Scheduler
   ↓
Cleans expired records from DB
```

while request-time validation:

```text
GET /{shortCode}
   ↓
Check expiresAt
   ↓
Reject expired URL
```

provides immediate protection even before the scheduler removes the record.

The scheduler runs only while the Spring Boot application is running.

---

# Concurrency & Thread Safety

Concurrency is a major design focus of this project.

Spring Boot can process multiple HTTP requests concurrently, so the application must correctly handle multiple requests accessing the same data at the same time.

---

## 5. Short Code Collision Protection

Short codes are generated dynamically.

Multiple requests could theoretically generate the same short code.

The database therefore enforces:

```text
UNIQUE(short_code)
```

This provides a database-level guarantee that two URL mappings cannot have the same short code.

The service also retries short-code generation when a database uniqueness violation occurs.

Maximum generation attempts:

```java
MAX_GENERATION_ATTEMPTS = 5
```

Flow:

```text
Generate short code
       ↓
Save to database
       ↓
Unique?
  ↓          ↓
 YES         NO
  ↓           ↓
Success    Generate another code
             ↓
           Retry
```

---

# Duplicate Original URL Protection

## 6. Same URL → Same Short Code

The service prevents multiple database records from being created for the same original URL.

For example:

```text
POST https://example.com
```

First request:

```text
https://example.com → Ab12Cd
```

A repeated request for the same URL returns:

```text
https://example.com → Ab12Cd
```

instead of generating another short code.

The database also enforces:

```text
UNIQUE(original_url)
```

This gives the system two important database-level guarantees:

```text
short_code    → UNIQUE
original_url  → UNIQUE
```

---

## Duplicate URL Request Flow

```text
POST /shorten
       ↓
Check original_url
       ↓
Does it already exist?
       │
   ┌───┴───┐
   │       │
  YES      NO
   │       │
   ↓       ↓
Return    Generate
existing  short code
code       │
   │        ↓
   │      INSERT
   │        │
   └────────┴──→ Response
```

When the URL already exists and has not expired:

```text
No new database row is created.
```

The API returns a message indicating that the existing short code is being returned.

Example:

```json
{
    "shortCode": "Ab12Cd",
    "originalUrl": "https://example.com",
    "message": "URL already shortened. Returning existing short code."
}
```

---

# Concurrent Duplicate Requests

A simple application-level check is not sufficient by itself.

For example, two requests could arrive simultaneously:

```text
Request A                    Request B
    ↓                            ↓
Check URL                  Check URL
    ↓                            ↓
Not found                  Not found
    ↓                            ↓
Generate code              Generate code
```

Both requests could believe that the URL does not exist.

Therefore, the database constraint is the final protection:

```text
UNIQUE(original_url)
```

Only one database row can exist for a particular original URL.

The service handles the resulting `DataIntegrityViolationException` and checks whether another concurrent request has already created the mapping.

This provides protection at two levels:

```text
Application Level
       +
Database Level
```

---

# Database Design

Current `url_mapping` table:

```sql
CREATE TABLE url_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    short_code VARCHAR(20) NOT NULL,

    original_url VARCHAR(2048) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    expires_at TIMESTAMP NULL,

    CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)
);
```

A Flyway migration adds the uniqueness constraint for original URLs:

```sql
ALTER TABLE url_mapping
ADD CONSTRAINT uk_url_mapping_original_url UNIQUE (original_url);
```

This is maintained as a new migration rather than modifying an already-applied Flyway migration.

---

# Current Project Structure

```text
src/main/java/com/example/urlshortener
│
├── config
│
├── concurrency
│
├── controller
│   └── UrlController
│
├── dto
│   ├── ShortenUrlRequest
│   └── ShortenUrlResponse
│
├── exception
│   ├── GlobalExceptionHandler
│   ├── RateLimitExceededException
│   └── UrlNotFoundException
│
├── model
│   └── UrlMapping
│
├── repository
│   └── UrlRepository
│
├── scheduler
│   └── UrlExpirationScheduler
│
└── service
    ├── ShortCodeGenerator
    └── UrlService
```

Database migrations:

```text
src/main/resources/db/migration
│
├── V1__create_url_mapping.sql
└── V2__add_unique_constraint_to_original_url.sql
```

---

# Current Request Flow

## Creating a URL

```text
Client
  ↓
POST /shorten
  ↓
UrlController
  ↓
UrlService
  ↓
Check original URL
  ↓
Existing?
 ┌───────┴───────┐
 YES             NO
 ↓                ↓
Return existing   Generate short code
short code        ↓
                  Save to MySQL
                      ↓
                  Return response
```

---

## Redirecting

```text
Client
  ↓
GET /{shortCode}
  ↓
UrlController
  ↓
UrlService
  ↓
UrlRepository
  ↓
Find short code
  ↓
Check expiration
  ↓
Return original URL
```

---

## Expiration Cleanup

```text
Spring Boot Application
        ↓
UrlExpirationScheduler
        ↓
Find expired mappings
        ↓
Delete expired records
        ↓
MySQL
```

---

# Concurrency Design Principles

The project currently follows these principles:

### Database constraints over application assumptions

Important uniqueness rules are enforced by MySQL rather than relying only on Java checks.

### Stateless service design

The service does not depend on mutable shared in-memory state for URL uniqueness.

### No unnecessary `synchronized`

The application does not use `synchronized` as the primary mechanism for protecting URL creation.

This is important because the eventual architecture is intended to support multiple application instances.

```text
Client
  ↓
Load Balancer
  ↓
┌─────────────┬─────────────┬─────────────┐
│ Spring Boot │ Spring Boot │ Spring Boot │
│ Instance 1  │ Instance 2  │ Instance 3  │
└─────────────┴─────────────┴─────────────┘
             ↓
           MySQL
```

A JVM-level lock would not protect requests across different application instances, while a database constraint can.

---

# Current Concurrency Progress

Completed:

* Concurrent request awareness
* Database uniqueness for `short_code`
* Short-code collision retry mechanism
* Duplicate original URL detection
* Database uniqueness for `original_url`
* Concurrent duplicate URL protection
* Stateless URL creation design
* Expiration timestamp validation
* Automatic expiration cleanup
* Scheduler-based cleanup

---

# Next Concurrency Work

The next stage focuses specifically on deeper database and distributed concurrency concepts:

1. Transactions
2. Transaction boundaries
3. Isolation levels
4. Race conditions
5. Concurrent request testing
6. Optimistic locking
7. Pessimistic locking
8. Database concurrency behavior
9. Multi-instance/distributed concurrency
10. Load balancing considerations

After the concurrency layer is understood and tested, the project can progress toward:

* Redis caching
* Rate limiting
* Docker
* Horizontal scaling
* Load balancing
* Database replication
* Database sharding
* Distributed-system design

---

# Project Goal

The goal is not just to build a basic URL shortener.

The project is being developed as a learning exercise for designing a **production-style distributed backend system**, with particular attention to:

* Concurrency
* Thread safety
* Database consistency
* Scalability
* Caching
* Fault tolerance
* Distributed systems
* Performance
* Clean architecture

The implementation is intentionally being built **one component at a time** so that each design decision and concurrency mechanism is understood before moving to the next stage.
