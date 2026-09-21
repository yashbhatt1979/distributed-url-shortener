# Distributed URL Shortener

A production-oriented URL Shortener built with **Java 17, Spring Boot, Maven, MySQL, Spring Data JPA, and Flyway**.

The project is being developed step-by-step with a focus on understanding the underlying concepts rather than simply implementing features.

---

## Today's Progress — Concurrency & Thread Safety

### Date

**21 September 2026**

Today we analyzed the existing URL Shortener from a **thread-safety and concurrency** perspective.

The main goal was to understand how multiple HTTP requests can execute concurrently and how to make the application safe without unnecessarily using Java's `synchronized` keyword.

---

# 1. Understanding Spring Boot Concurrency

Spring Boot applications can process multiple HTTP requests concurrently.

Conceptually:

```text
Request 1 ──→ Thread 1 ──┐
Request 2 ──→ Thread 2 ──┤
Request 3 ──→ Thread 3 ──┼──→ Spring Boot Application
Request 4 ──→ Thread 4 ──┘
```

Therefore, application components must be designed so that concurrent requests do not corrupt shared state.

The main principle established today was:

> **Avoid shared mutable state instead of automatically using `synchronized`.**

---

# 2. UrlController — Thread Safety

The `UrlController` was reviewed.

Current structure:

```text
UrlController
     ↓
UrlService
```

The controller contains only:

```java
private final UrlService urlService;
```

and request-specific local variables.

There is no shared mutable request state.

### Result

**No changes required.**

The controller can safely handle concurrent requests.

We deliberately did **not** use:

```java
synchronized
```

on controller methods because that would unnecessarily serialize incoming requests.

---

# 3. UrlService — Concurrency

The `UrlService` was analyzed as the main application-level component.

The service is effectively **stateless**:

```text
UrlService
├── UrlRepository
├── ShortCodeGenerator
└── expirationHours
```

There is no mutable request-specific state stored inside the service.

### Short-code collision handling

A concurrency problem can theoretically occur:

```text
Thread A → generates ABC123
Thread B → generates ABC123
```

Both may attempt:

```text
INSERT ABC123
```

The solution is **not** a global Java lock.

Instead:

```text
Generate short code
       ↓
INSERT into MySQL
       ↓
UNIQUE(short_code)
       ↓
 ┌─────┴─────┐
 ↓           ↓
Success    Collision
             ↓
       Generate again
```

The service was updated to retry short-code generation when a database uniqueness violation occurs.

A maximum retry limit of **5 attempts** was introduced to prevent an endless retry loop.

---

# 4. ShortCodeGenerator — Thread Safety

The `ShortCodeGenerator` was reviewed.

It uses:

```java
private final SecureRandom random = new SecureRandom();
```

and:

```java
StringBuilder shortCode = new StringBuilder(CODE_LENGTH);
```

### Why this is safe

`SecureRandom` supports concurrent use.

The `StringBuilder` is created inside:

```java
generateShortCode()
```

Therefore every invocation gets its own `StringBuilder`.

For example:

```text
Thread 1 → StringBuilder A
Thread 2 → StringBuilder B
Thread 3 → StringBuilder C
```

The builders are not shared between threads.

### Result

**No changes required.**

We did not add `synchronized`.

---

# 5. Why We Are Not Using `synchronized`

An important concept established today:

```java
synchronized
```

is not a universal solution to concurrency.

If we synchronized:

```java
public synchronized ShortenUrlResponse shortenUrl(...)
```

then:

```text
Thread 1 → executes
Thread 2 → WAIT
Thread 3 → WAIT
Thread 4 → WAIT
```

This unnecessarily reduces concurrency.

More importantly, `synchronized` only protects threads within **one JVM**.

Eventually our distributed architecture may look like:

```text
              Load Balancer
             /      |      \
            ↓       ↓       ↓
        Instance A B       C
```

A lock on Instance A does not protect Instance B or C.

Therefore, database-level and distributed coordination mechanisms are more appropriate for shared resources.

---

# 6. UrlRepository — Concurrency

The repository extends:

```java
JpaRepository<UrlMapping, Long>
```

with:

```java
Optional<UrlMapping> findByShortCode(String shortCode);

void deleteByExpiresAtBefore(LocalDateTime time);
```

### Result

No Java synchronization is required.

Spring Data JPA and the database handle concurrent database operations.

The repository itself does not maintain application-level mutable state that needs manual synchronization.

---

# 7. Database-Level Uniqueness

Our Flyway migration already contains:

```sql
CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)
```

This is one of the most important concurrency protections in the application.

It guarantees that two concurrent requests cannot successfully insert the same short code.

Example:

```text
Thread A → INSERT abc123 → SUCCESS

Thread B → INSERT abc123 → UNIQUE constraint violation
                              ↓
                         Retry with new code
```

This protection works even when the application eventually runs across multiple Spring Boot instances.

---

# 8. UrlMapping — Optimistic Locking

We investigated whether to add:

```java
@Version
private Long version;
```

for optimistic locking.

### Decision

**Not added yet.**

Our current URL lifecycle is primarily:

```text
POST → INSERT
GET  → SELECT
Scheduler → DELETE
```

We currently do not have multiple concurrent requests modifying the same `UrlMapping` record.

Therefore, adding `@Version` at this stage would introduce a mechanism before we actually need it.

Optimistic locking will become relevant when we introduce features such as:

* URL updates
* click counters
* analytics counters
* metadata modifications
* other concurrent updates to the same row

---

# 9. UrlExpirationScheduler — Concurrency

The expiration scheduler was reviewed because it performs database deletes while HTTP requests may simultaneously read URLs.

The scheduler was updated to use:

```java
@Transactional
```

The current flow is:

```text
Every 60 seconds
       ↓
deleteExpiredUrls()
       ↓
BEGIN TRANSACTION
       ↓
DELETE expired URLs
       ↓
COMMIT
```

If the database operation fails:

```text
BEGIN
  ↓
DELETE
  ↓
ERROR
  ↓
ROLLBACK
```

---

# 10. Expiration Design

An important design decision was established:

The scheduler is **not responsible for deciding whether a URL is valid**.

The request itself checks:

```text
expiresAt
```

Therefore:

```text
URL expires at 4:00 PM
Scheduler runs at 4:01 PM
```

A request arriving at:

```text
4:00:30 PM
```

can still correctly determine that the URL has expired even though the scheduler has not deleted it yet.

We therefore have two mechanisms:

```text
             URL expiration
                   │
          ┌────────┴────────┐
          ↓                 ↓
     Request check      Scheduler cleanup
          │                 │
          ↓                 ↓
   Reject expired       Delete old rows
```

This separates **correctness** from **cleanup**.

---

# 11. Current Concurrency Architecture

The current architecture is:

```text
                    HTTP Requests
                         │
             ┌───────────┴───────────┐
             ↓                       ↓
       POST /shortenUrl       GET /shortenUrl/{code}
             │                       │
             ↓                       ↓
       UrlController            UrlController
             │                       │
             └───────────┬───────────┘
                         ↓
                     UrlService
                         │
              ┌──────────┴──────────┐
              ↓                     ↓
      ShortCodeGenerator      UrlRepository
                                    │
                                    ↓
                                  MySQL
                                    │
                         UNIQUE(short_code)
                                    ↑
                                    │
                         Expiration Scheduler
```

The design intentionally avoids unnecessary Java-level locking.

---

# 12. Current Thread-Safety Status

| Component                | Status                           | Reason                                  |
| ------------------------ | -------------------------------- | --------------------------------------- |
| `UrlController`          | ✅ Safe                           | No shared mutable request state         |
| `UrlService`             | ✅ Safe                           | Stateless design                        |
| `ShortCodeGenerator`     | ✅ Safe                           | `SecureRandom` + local `StringBuilder`  |
| `UrlRepository`          | ✅ Safe                           | Spring Data JPA/database infrastructure |
| `UrlMapping`             | ✅ Currently sufficient           | No concurrent updates yet               |
| `UrlExpirationScheduler` | ✅ Improved                       | Transactional cleanup                   |
| MySQL schema             | ✅ Safe for short-code uniqueness | `UNIQUE(short_code)`                    |

---

# 13. What We Have NOT Covered Yet

Today's work covers the **basic thread-safety layer**.

Concurrency is not finished.

The next stage will focus heavily on **database concurrency**.

Topics to study:

### Transactions

```text
BEGIN
   ↓
Operations
   ↓
COMMIT / ROLLBACK
```

### Isolation Levels

```text
READ UNCOMMITTED
READ COMMITTED
REPEATABLE READ
SERIALIZABLE
```

### Race Conditions

Including:

* Lost updates
* Dirty reads
* Non-repeatable reads
* Phantom reads

### Locking

```text
Optimistic Locking
        ↓
@Version
```

and:

```text
Pessimistic Locking
        ↓
Database row locks
SELECT ... FOR UPDATE
```

---

# 14. Future Distributed Concurrency

After understanding database concurrency, the project will move toward distributed concurrency.

The eventual architecture is expected to involve:

```text
                    Load Balancer
                   /      |      \
                  ↓       ↓       ↓
             Instance A Instance B Instance C
                  │       │       │
                  └───────┼───────┘
                          ↓
                        Redis
                          ↓
                        MySQL
```

Future topics include:

* Redis atomic operations
* Distributed locks
* Idempotency
* Race conditions across multiple application instances
* Cache consistency
* Rate limiting
* Database contention
* Horizontal scaling
* Replication
* Read/write separation
* Eventual consistency

---

# Today's Key Takeaways

### 1. Thread safety does not mean using `synchronized` everywhere.

### 2. Stateless Spring beans are naturally easier to make thread-safe.

### 3. Database constraints are essential for concurrency.

### 4. `UNIQUE(short_code)` is our authoritative protection against duplicate short codes.

### 5. A database constraint + retry is better than globally locking short-code generation.

### 6. `SecureRandom` is safe for concurrent use.

### 7. Local variables such as `StringBuilder` do not create shared-state problems.

### 8. `@Version` is useful for concurrent updates, but our current URL lifecycle doesn't require it yet.

### 9. Expiration validation happens during the request; the scheduler performs cleanup.

### 10. Java-level synchronization does not solve distributed concurrency.

---

# Next Session

**Focus: Database Concurrency**

We will continue with concurrency only.

Planned order:

```text
Java Threads
     ↓
Race Conditions
     ↓
Transactions
     ↓
Isolation Levels
     ↓
Lost Updates
     ↓
Optimistic Locking
     ↓
Pessimistic Locking
     ↓
Concurrent Testing
     ↓
Distributed Concurrency
```

The goal is to understand **why each mechanism is needed** and then implement only the mechanisms that our URL Shortener actually requires.
