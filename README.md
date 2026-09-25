# Distributed URL Shortener

A distributed URL shortener built using **Java 17, Spring Boot, MySQL, Redis, Docker, and Nginx**.

The project focuses not only on URL shortening but also on important backend and distributed-systems concepts such as:

* Concurrency
* Thread Safety
* Redis Caching
* Distributed Locks
* Rate Limiting
* Horizontal Scaling
* Load Balancing
* Database Replication
* Docker Containerization

---

# Architecture

Current architecture:

```text
                         Client
                           |
                           v
                    +-------------+
                    |    Nginx    |
                    |Load Balancer|
                    +-------------+
                       /   |   \
                      /    |    \
                     v     v     v
                  App-1  App-2  App-3
                    |      |      |
                    +------+------+
                           |
                    +------+------+
                    |             |
                    v             v
                  Redis        MySQL
                                |
                                v
                         MySQL Replica
```

---

# Features Implemented

## 1. Redis Token Bucket Rate Limiting

A distributed rate limiter was implemented using **Redis** and the **Token Bucket algorithm**.

The rate limiter protects the API from excessive requests and ensures that multiple application instances can share the same rate-limit state.

### Why Redis?

Because the application runs multiple instances:

```text
App-1
App-2
App-3
```

Using an in-memory rate limiter inside each application would create separate limits:

```text
App-1 → separate bucket
App-2 → separate bucket
App-3 → separate bucket
```

This would allow a client to potentially bypass the intended global limit by hitting different instances.

Redis provides a shared state:

```text
             +---------+
             |  Redis  |
             +---------+
              /   |   \
             /    |    \
          App-1 App-2 App-3
```

All instances therefore use the same rate-limit information.

---

# Token Bucket Algorithm

The Token Bucket algorithm maintains a bucket containing tokens.

Conceptually:

```text
             Token Bucket
          +---------------+
          | ● ● ● ● ●     |
          |               |
          | capacity = N  |
          +---------------+
                  |
             Request
                  |
          Token available?
             /       \
           Yes        No
            |          |
       Allow request   Reject
                      HTTP 429
```

A request consumes one token.

Tokens are replenished over time according to the configured refill rate.

When the bucket has no tokens available, the request is rejected.

---

# Rate Limiting Response

When the rate limit is exceeded, the API returns:

```http
HTTP/1.1 429 Too Many Requests
```

with the configured error message.

This allows clients to distinguish rate limiting from other application errors.

---

# Rate Limiting Components

The rate-limiting implementation contains components responsible for:

```text
RateLimiter
RateLimitService
RateLimitExceededException
TokenBucket
Bucket
```

The application uses Redis as the shared state store.

---

# Redis Configuration

Redis is running as a Docker container:

```yaml
redis:
  image: redis:7-alpine
```

It is exposed on:

```text
6379
```

The Spring Boot applications connect to Redis using:

```text
REDIS_HOST=redis
REDIS_PORT=6379
```

Inside the Docker network, the applications communicate with Redis using the Docker service name:

```text
redis
```

---

# 2. Load Balancing

Nginx was added as a **reverse proxy and load balancer**.

Instead of clients directly communicating with a single Spring Boot instance:

```text
Client
   |
   v
App-1
```

the client now communicates with Nginx:

```text
Client
   |
   v
Nginx
   |
   +----> App-1
   |
   +----> App-2
   |
   +----> App-3
```

---

# Why Load Balancing?

The application is horizontally scaled to three Spring Boot instances:

```text
App-1
App-2
App-3
```

Without a load balancer, clients would need to know which application instance to contact.

Nginx provides a single entry point and distributes incoming requests among the available instances.

Benefits include:

* Distribution of incoming traffic
* Horizontal scalability
* Better utilization of application instances
* A single public entry point
* Improved fault tolerance when multiple instances are available

---

# Nginx Configuration

Nginx was added using the official Docker image:

```yaml
nginx:
  image: nginx:latest
```

Nginx uses the project configuration file:

```text
nginx/
└── nginx.conf
```

The configuration defines the Spring Boot instances as backend servers:

```nginx
upstream backend_servers {

    server app-1:8080;
    server app-2:8080;
    server app-3:8080;

}
```

Requests are forwarded to the backend group using:

```nginx
proxy_pass http://backend_servers;
```

---

# Load Balancing Algorithm

Nginx uses **Round Robin** by default for the configured upstream servers.

Conceptually:

```text
Request 1 → App-1
Request 2 → App-2
Request 3 → App-3
Request 4 → App-1
Request 5 → App-2
Request 6 → App-3
```

This allows incoming requests to be distributed across the application instances.

---

# Nginx Port

Port `80` on the host machine was already unavailable.

Therefore, Nginx was exposed using:

```yaml
ports:
  - "8081:80"
```

This means:

```text
Host:
localhost:8081

        ↓

Docker:
Nginx:80
```

Nginx itself continues to listen on port `80` inside the container.

---

# Request Flow

The current request flow is:

```text
Postman / Client
       |
       v
localhost:8081
       |
       v
Nginx :80
       |
       v
+------+------+------+
|      |             |
v      v             v
App-1 App-2        App-3
 :8080 :8080        :8080
       |
       v
 Redis / MySQL
```

The client does not need to know which Spring Boot instance handles the request.

---

# Docker Services

The current Docker Compose setup contains:

```text
mysql-primary
mysql-replica
redis
app-1
app-2
app-3
nginx
```

### MySQL Primary

```text
Host: 3307
Container: 3306
```

### MySQL Replica

```text
Host: 3308
Container: 3306
```

### Redis

```text
Host: 6379
Container: 6379
```

### Spring Boot App-1

```text
Host: 8080
Container: 8080
```

### Spring Boot App-2

```text
Container: 8080
```

### Spring Boot App-3

```text
Container: 8080
```

### Nginx

```text
Host: 8081
Container: 80
```

---

# Docker Network

All services communicate through the Docker bridge network:

```text
url-shortener-network
```

The applications can therefore communicate using Docker service names.

For example:

```text
app-1 → redis:6379
app-1 → mysql-primary:3306
nginx → app-1:8080
nginx → app-2:8080
nginx → app-3:8080
```

No container needs to know the IP address of another container.

Docker's internal DNS resolves the service names.

---

# Files Added / Modified

## New Files

```text
nginx/
└── nginx.conf
```

Rate limiting components:

```text
RateLimiter
RateLimitService
RateLimitExceededException
TokenBucket
Bucket
```

---

## Modified Files

```text
docker-compose.yml
```

The Docker Compose file was updated to include:

```text
app-1
app-2
app-3
nginx
```

along with Redis and MySQL services.

---

# Testing

## Rate Limiting Test

Send repeated requests to the API.

When the configured token bucket is exhausted, the application should return:

```http
429 Too Many Requests
```

Example flow:

```text
Request
   |
   v
Rate Limiter
   |
   +---- Token available → Continue
   |
   +---- No token → HTTP 429
```

---

# Load Balancing Test

The load balancer can be accessed through:

```text
http://localhost:8081
```

For example:

```http
POST http://localhost:8081/shortenUrl
```

The request path becomes:

```text
Client
  ↓
Nginx
  ↓
App-1 / App-2 / App-3
  ↓
Application
  ↓
Redis / MySQL
```

---

# Useful Docker Commands

### Start all services

```bash
docker compose up -d --build
```

### Stop all services

```bash
docker compose down
```

### Check service status

```bash
docker compose ps
```

### Check Nginx logs

```bash
docker logs url-shortener-nginx
```

### Check application logs

```bash
docker logs url-shortener-app-1
```

```bash
docker logs url-shortener-app-2
```

```bash
docker logs url-shortener-app-3
```

### Start only Nginx

```bash
docker compose up -d nginx
```

### Check available Compose services

```bash
docker compose config --services
```

---

# Concepts Learned

Today's implementation covered several important backend and distributed-systems concepts.

### Rate Limiting

* Token Bucket
* Shared rate-limit state
* Redis-based distributed rate limiting
* HTTP `429 Too Many Requests`

### Load Balancing

* Reverse Proxy
* Nginx
* Horizontal Scaling
* Round Robin
* Backend server pools
* Docker service discovery

### Docker

* Multi-container applications
* Docker networks
* Service names
* Container-to-container communication
* Port mapping
* Docker Compose

---

# Current Architecture

```text
                         CLIENT
                           |
                           |
                    localhost:8081
                           |
                           v
                 +-------------------+
                 |       NGINX       |
                 | Reverse Proxy +   |
                 | Load Balancer      |
                 +-------------------+
                    /      |      \
                   /       |       \
                  v        v        v
              +------+ +------+ +------+
              |App-1 | |App-2 | |App-3 |
              +------+ +------+ +------+
                  \        |        /
                   \       |       /
                    +------+------+
                           |
              +------------+------------+
              |                         |
              v                         v
          +--------+              +-----------+
          | Redis  |              |   MySQL   |
          |        |              |  Primary  |
          +--------+              +-----------+
                                        |
                                        v
                                  +-----------+
                                  |   MySQL   |
                                  |  Replica  |
                                  +-----------+
```

---

# Next Steps

The next improvements to the distributed URL shortener are:

* Verify Round Robin distribution between all three application instances
* Add application-instance identification for load-balancing testing
* Test failure handling when one application instance goes down
* Configure Nginx backend health/failure handling
* Improve Nginx configuration
* Complete database replication verification
* Implement/verify database read/write separation
* Further improve Redis caching
* Container health checks
* Production-oriented Docker configuration
* Final deployment

---

# Technology Stack

| Technology        | Purpose                                   |
| ----------------- | ----------------------------------------- |
| Java 17           | Programming language                      |
| Spring Boot       | Backend framework                         |
| Maven             | Build management                          |
| MySQL             | Primary database                          |
| MySQL Replication | Database redundancy                       |
| Redis             | Caching, distributed state, rate limiting |
| Nginx             | Reverse proxy and load balancer           |
| Docker            | Containerization                          |
| Docker Compose    | Multi-container orchestration             |
| Flyway            | Database migrations                       |
| Postman           | API testing                               |
| Git/GitHub        | Version control                           |

---

# Project Goal

The goal of this project is to build a **scalable, distributed URL shortener** that demonstrates real backend engineering concepts rather than only basic CRUD functionality.

The system progressively incorporates:

```text
REST API
   ↓
Database
   ↓
Caching
   ↓
Concurrency
   ↓
Distributed Lock
   ↓
Rate Limiting
   ↓
Horizontal Scaling
   ↓
Load Balancing
   ↓
Database Replication
   ↓
Containerization
   ↓
Deployment
```
