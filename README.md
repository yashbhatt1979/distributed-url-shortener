# 🚀 Distributed URL Shortener — Redis Caching & Dockerization

## 📅 Work Completed — 23 September 2026

Today, the URL Shortener project was extended with **Redis caching** and fully **dockerized** so that the application, MySQL database, and Redis can run together as containers.

---

# 🔴 1. Redis Caching

Redis was integrated into the URL Shortener to improve the performance of URL redirection.

### Why Redis?

Without caching, every request to a shortened URL requires a database lookup:

```text
Client
  ↓
Spring Boot
  ↓
MySQL
  ↓
Original URL
```

With Redis caching:

```text
Client
  ↓
Spring Boot
  ↓
Redis Cache
  ↓
Original URL
```

If the URL is present in Redis, the application can avoid querying MySQL.

---

## Redis Cache Flow

### First Request — Cache Miss

```text
GET /shortenUrl/abc123
        ↓
Check Redis
        ↓
Not Found
        ↓
Query MySQL
        ↓
Original URL found
        ↓
Store URL in Redis
        ↓
Redirect user
```

### Subsequent Request — Cache Hit

```text
GET /shortenUrl/abc123
        ↓
Check Redis
        ↓
URL Found
        ↓
Redirect user
```

This reduces unnecessary database queries for frequently accessed URLs.

---

# 🧠 Cache-Aside Strategy

The project uses the **Cache-Aside** pattern.

The application:

1. Checks Redis first.
2. If the value exists → return it.
3. If Redis misses → query MySQL.
4. Store the result in Redis.
5. Return the original URL.

Conceptually:

```java
String originalUrl = redis.get(shortCode);

if (originalUrl != null) {
    return originalUrl; // Cache Hit
}

originalUrl = database.find(shortCode);

redis.set(shortCode, originalUrl); // Cache

return originalUrl;
```

---

# 🐳 2. Dockerization

The application was dockerized so that the complete system can run without manually installing and configuring every dependency.

The project now consists of three main containers:

```text
                 ┌─────────────────┐
                 │   Spring Boot   │
                 │   Application   │
                 └────────┬────────┘
                          │
              ┌───────────┴───────────┐
              │                       │
              ▼                       ▼
       ┌─────────────┐        ┌─────────────┐
       │    MySQL    │        │    Redis    │
       │   Database  │        │    Cache    │
       └─────────────┘        └─────────────┘
```

---

# 📦 Dockerfile

A `Dockerfile` was created to package the Spring Boot application into a Docker image.

Its responsibilities include:

* Selecting the Java runtime environment.
* Copying the application JAR.
* Defining the application startup command.
* Creating a reproducible environment for the application.

Conceptually:

```text
Dockerfile
     ↓
Build Docker Image
     ↓
Spring Boot Application Image
     ↓
Run Container
```

---

# 🐳 docker-compose.yaml

A `docker-compose.yaml` file was created to manage the complete application stack.

It defines containers for:

### 1. Spring Boot

Runs the URL Shortener application.

### 2. MySQL

Stores URL mappings and application data.

### 3. Redis

Stores cached URL mappings.

The containers communicate through the Docker Compose network.

```text
Spring Boot
     │
     ├──────────► MySQL
     │
     └──────────► Redis
```

---

# 🗄️ MySQL Container

MySQL is now running inside a Docker container.

Configuration includes:

```yaml
mysql:
  image: mysql:8.0
```

The database uses a Docker volume:

```text
mysql_data
     ↓
/var/lib/mysql
```

This allows MySQL data to persist even when the container is stopped or recreated.

---

# 🔴 Redis Container

Redis is also running as a Docker container.

The Spring Boot application communicates with Redis through the Docker Compose service name rather than `localhost`.

Inside Docker:

```text
Redis host = redis
Redis port = 6379
```

This is important because:

```text
localhost
```

inside the Spring Boot container refers to the Spring Boot container itself, not the Redis container.

---

# 🔐 Environment Variables

Sensitive configuration was moved toward environment-based configuration using a `.env` file.

The purpose of `.env` is to avoid hardcoding sensitive configuration directly inside `docker-compose.yaml`.

Typical configuration includes:

```env
MYSQL_ROOT_PASSWORD=your_password
MYSQL_DATABASE=url_shortener
MYSQL_USER=your_user
MYSQL_PASSWORD=your_password
```

The `.env` file should **not be committed to GitHub**.

It should be included in `.gitignore`:

```gitignore
.env
```

A safe template can instead be provided:

```text
.env.example
```

---

# 🌐 Docker Networking

Docker Compose automatically creates a network for the services.

The application can therefore communicate with:

```text
mysql:3306
redis:6379
```

instead of using:

```text
localhost:3306
localhost:6379
```

The architecture becomes:

```text
                 Docker Network
┌─────────────────────────────────────────┐
│                                         │
│  ┌──────────────┐                       │
│  │ Spring Boot  │                       │
│  │    :8080     │                       │
│  └──────┬───────┘                       │
│         │                                │
│    ┌────┴─────┐                          │
│    │          │                          │
│    ▼          ▼                          │
│  MySQL      Redis                        │
│  :3306      :6379                        │
│                                         │
└─────────────────────────────────────────┘
```

---

# 🏥 Health Checks

Docker Compose was also configured with health checks, particularly for MySQL.

The purpose of a health check is to verify that a service is actually ready to accept connections.

This is different from simply checking whether the container process is running.

```text
Container Running
       ↓
Health Check
       ↓
Database Ready
```

This helps prevent the Spring Boot application from trying to connect to MySQL before MySQL is ready.

---

# 🧪 Testing

The Redis integration was tested through the URL Shortener API.

### Create Short URL

```http
POST /shortenUrl
```

Example request:

```json
{
    "originalUrl": "https://www.example.com"
}
```

The application generates a short code and stores the mapping in MySQL.

---

## Test Redis Caching

After creating a shortened URL:

```http
GET /shortenUrl/{shortCode}
```

### First request

```text
Redis → MISS
MySQL → Query
Redis → Store result
Redirect
```

### Second request

```text
Redis → HIT
Redirect
```

The second request should be served from Redis rather than requiring another MySQL lookup.

---

# 🐳 Running the Project with Docker

Build and start all services:

```bash
docker compose up --build
```

Run in detached mode:

```bash
docker compose up --build -d
```

Check running containers:

```bash
docker ps
```

View application logs:

```bash
docker compose logs app
```

View Redis logs:

```bash
docker compose logs redis
```

View MySQL logs:

```bash
docker compose logs mysql
```

Stop the complete application:

```bash
docker compose down
```

---

# 🧹 Removing Containers and Volumes

To stop containers and remove the containers:

```bash
docker compose down
```

To also remove persistent volumes:

```bash
docker compose down -v
```

⚠️ Removing volumes will delete the stored MySQL data.

---

# 📁 Important Project Files

The project now contains the following important infrastructure files:

```text
url-shortener/
│
├── src/
│   └── main/
│       └── java/
│
├── Dockerfile
├── docker-compose.yaml
├── .env
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

---

# 🏗️ Current Architecture

The project has evolved into:

```text
                         Client
                           │
                           ▼
                    ┌─────────────┐
                    │ Spring Boot │
                    │   REST API  │
                    └──────┬──────┘
                           │
                 ┌─────────┴─────────┐
                 │                   │
                 ▼                   ▼
             ┌───────┐          ┌────────┐
             │ Redis │          │ MySQL  │
             │ Cache │          │   DB   │
             └───────┘          └────────┘
```

### Request flow

```text
POST /shortenUrl
       │
       ▼
   Spring Boot
       │
       ▼
     MySQL
       │
       ▼
 Short URL Created
```

For redirection:

```text
GET /shortenUrl/{code}
       │
       ▼
    Redis?
    /    \
  HIT    MISS
   │       │
   │       ▼
   │     MySQL
   │       │
   │       ▼
   │     Redis
   │       │
   └───┬───┘
       ▼
    Redirect
```

---

# 🎯 What Was Learned Today

### Redis

* What caching is.
* Why caching improves application performance.
* Cache hit vs cache miss.
* Cache-Aside pattern.
* Using Redis with Spring Boot.
* Running Redis through Docker.
* Using Redis as a distributed cache.

### Docker

* What Docker containers are.
* Difference between a Docker image and container.
* Purpose of a `Dockerfile`.
* Purpose of `docker-compose.yaml`.
* Container networking.
* Service names in Docker Compose.
* Docker volumes.
* Environment variables.
* Container health checks.
* Running a multi-container application.

---

# 🚀 Project Progress

### Completed

* ✅ Spring Boot REST API
* ✅ MySQL persistence
* ✅ Flyway database migrations
* ✅ URL expiration
* ✅ Scheduled URL cleanup
* ✅ Duplicate URL handling
* ✅ Global exception handling
* ✅ Rate limiting
* ✅ Redis caching
* ✅ Dockerfile
* ✅ Docker Compose
* ✅ MySQL container
* ✅ Redis container
* ✅ Spring Boot container
* ✅ Docker networking
* ✅ Environment-based configuration
* ✅ Container health checks

### Next Steps

The next stage can focus on making the system more **distributed and scalable**, including:

* Redis TTL and cache eviction
* Cache invalidation
* Concurrency and race-condition handling
* Distributed rate limiting
* Docker optimization
* Horizontal scaling
* Load balancing
* Database indexing optimization
* Database replication
* Sharding
* Distributed-system consistency
* Production deployment

---

## 📌 Current Architecture Goal

The long-term goal is to evolve the project from a basic URL shortener into a production-style distributed system:

```text
                         ┌──────────────┐
                         │    Client    │
                         └───────┬──────┘
                                 │
                                 ▼
                         ┌──────────────┐
                         │Load Balancer │
                         └───────┬──────┘
                                 │
                  ┌──────────────┼──────────────┐
                  │              │              │
                  ▼              ▼              ▼
             ┌────────┐     ┌────────┐     ┌────────┐
             │App #1  │     │App #2  │     │App #3  │
             └───┬────┘     └───┬────┘     └───┬────┘
                 │              │              │
                 └──────────────┼──────────────┘
                                │
                         ┌──────▼──────┐
                         │    Redis    │
                         │    Cache    │
                         └──────┬──────┘
                                │
                         ┌──────▼──────┐
                         │    MySQL    │
                         │   Cluster   │
                         └─────────────┘
```

This establishes the foundation for turning the URL Shortener into a **scalable distributed backend system**.
