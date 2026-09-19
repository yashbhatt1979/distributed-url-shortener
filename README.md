# Distributed URL Shortener

A backend-focused URL shortening service built with **Java 17, Spring Boot, Maven, and MySQL**.

The project is being developed step-by-step with a focus on understanding and implementing the foundations required for a **scalable, concurrent, and distributed backend system**.

---

## 🚧 Project Status

**Current Stage:** Initial project structure and backend architecture

The initial layered structure has been created and pushed to GitHub. The classes and packages currently define the intended architecture, while the core business logic and infrastructure features are being implemented incrementally.

### Current Technology Stack

* **Java 17**
* **Spring Boot**
* **Maven**
* **MySQL**
* **Spring Web**
* **Git & GitHub**

---

## 📁 Project Structure

```text
distributed-url-shortener/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── .../
│   │   │       └── urlshortener/
│   │   │           │
│   │   │           ├── config/
│   │   │           │
│   │   │           ├── controller/
│   │   │           │   └── UrlController.java
│   │   │           │
│   │   │           ├── dto/
│   │   │           │   ├── ShortenUrlRequest.java
│   │   │           │   └── ShortenUrlResponse.java
│   │   │           │
│   │   │           ├── exception/
│   │   │           │   ├── GlobalExceptionHandler.java
│   │   │           │   └── UrlNotFoundException.java
│   │   │           │
│   │   │           ├── model/
│   │   │           │   └── UrlMapping.java
│   │   │           │
│   │   │           ├── repository/
│   │   │           │   └── UrlRepository.java
│   │   │           │
│   │   │           ├── service/
│   │   │           │   └── UrlService.java
│   │   │           │
│   │   │           └── UrlShortenerApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## 🏗️ Architecture

The project follows a layered backend architecture:

```text
Client
  │
  ▼
Controller
  │
  ▼
Service
  │
  ▼
Repository
  │
  ▼
MySQL
```

### Layers

#### Controller

Responsible for handling HTTP requests and returning HTTP responses.

```text
controller/
└── UrlController.java
```

#### Service

Contains the application's business logic.

```text
service/
└── UrlService.java
```

#### Repository

Responsible for database interaction.

```text
repository/
└── UrlRepository.java
```

#### Model

Represents the application's data model.

```text
model/
└── UrlMapping.java
```

#### DTO

Data Transfer Objects are used to define the structure of data exchanged through the API.

```text
dto/
├── ShortenUrlRequest.java
└── ShortenUrlResponse.java
```

#### Exception

Centralized exception-related components.

```text
exception/
├── GlobalExceptionHandler.java
└── UrlNotFoundException.java
```

#### Configuration

Configuration-related components will be placed here as the project grows.

```text
config/
```

---

## 🎯 Project Goal

The long-term goal is to build a URL shortener that goes beyond a basic CRUD application and demonstrates concepts used in production backend systems.

The planned system will progressively cover:

* REST API design
* URL shortening
* URL redirection
* Java concurrency
* Thread safety
* Database indexing
* Transactions
* Redis caching
* Rate limiting
* Distributed systems
* Horizontal scaling
* Load balancing
* Database replication
* Database sharding
* Consistent hashing
* Docker
* Containerization
* Monitoring and observability

These features are part of the planned development roadmap and are **not all implemented yet**.

---

## 🛠️ Development Approach

The project is intentionally being developed incrementally.

Instead of building the entire application at once, each component is being implemented after understanding the underlying concept.

The development progression is broadly:

```text
Backend Foundations
        │
        ▼
HTTP & REST
        │
        ▼
Java Concurrency
        │
        ▼
Thread Safety
        │
        ▼
Networking
        │
        ▼
Database Fundamentals
        │
        ▼
URL Shortener Core
        │
        ▼
Caching & Redis
        │
        ▼
Rate Limiting
        │
        ▼
Distributed Systems
        │
        ▼
Docker & Deployment
        │
        ▼
Scaling & High Availability
```

---

## 📋 Current Components

| Component                 | Status               |
| ------------------------- | -------------------- |
| Spring Boot project       | ✅ Created            |
| Java 17                   | ✅ Configured         |
| Maven                     | ✅ Configured         |
| Layered package structure | ✅ Created            |
| Controller layer          | 🟡 Structure created |
| Service layer             | 🟡 Structure created |
| Repository layer          | 🟡 Structure created |
| DTOs                      | 🟡 Structure created |
| Model                     | 🟡 Structure created |
| Exception layer           | 🟡 Structure created |
| MySQL integration         | 🔄 To be implemented |
| URL generation logic      | 🔄 To be implemented |
| URL redirection           | 🔄 To be implemented |
| Validation                | 🔄 To be implemented |
| Concurrency handling      | 🔄 To be implemented |
| Redis caching             | 🔄 Planned           |
| Rate limiting             | 🔄 Planned           |
| Docker                    | 🔄 Planned           |
| Horizontal scaling        | 🔄 Planned           |
| Database replication      | 🔄 Planned           |
| Database sharding         | 🔄 Planned           |

---

## 🚀 Running the Project

### Prerequisites

Make sure the following are installed:

* Java 17
* Maven
* MySQL
* Git

### Clone the Repository

```bash
git clone <repository-url>
cd distributed-url-shortener
```

### Run Using Maven Wrapper

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

---

## 🔀 Git Workflow

The project is maintained using Git and GitHub.

The basic workflow is:

```text
Modify Code
    │
    ▼
git status
    │
    ▼
git add .
    │
    ▼
git commit
    │
    ▼
git push
    │
    ▼
GitHub Repository
```

---

## 📚 Learning Objectives

This project is also being used as a practical way to learn backend engineering concepts.

Key concepts include:

### Backend

* HTTP
* REST APIs
* JSON
* Spring Boot
* Layered architecture
* Dependency Injection

### Java

* OOP
* Collections
* Multithreading
* Executors
* Concurrency
* Synchronization
* Thread safety
* Concurrent data structures

### Networking

* DNS
* TCP/IP
* Ports
* Sockets
* Reverse proxies
* Load balancers

### Databases

* SQL
* Indexes
* Transactions
* Database concurrency
* Replication
* Sharding

### Distributed Systems

* Scalability
* Availability
* Consistency
* CAP theorem
* Distributed caching
* Consistent hashing
* Horizontal scaling

---

## 🔮 Future Architecture

As the project evolves, the architecture is expected to progress from:

```text
Client
   │
   ▼
Spring Boot Application
   │
   ▼
MySQL
```

toward a more distributed architecture:

```text
                    ┌───────────────┐
                    │    Client     │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ Load Balancer │
                    └───────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
              ▼             ▼             ▼
          ┌────────┐    ┌────────┐    ┌────────┐
          │ Server │    │ Server │    │ Server │
          │   1    │    │   2    │    │   3    │
          └───┬────┘    └───┬────┘    └───┬────┘
              │             │             │
              └─────────────┼─────────────┘
                            │
                    ┌───────▼───────┐
                    │     Redis     │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │    MySQL      │
                    │  Replication  │
                    └───────────────┘
```

This represents the **planned direction of the project**, not the current implementation.

---

## 📌 Roadmap

* [x] Create Spring Boot project
* [x] Configure Java 17
* [x] Configure Maven
* [x] Create initial package structure
* [x] Create core classes
* [x] Initialize Git repository
* [x] Connect project to GitHub
* [x] Push initial project structure
* [ ] Implement URL shortening
* [ ] Implement URL retrieval
* [ ] Implement URL redirection
* [ ] Connect MySQL
* [ ] Add database indexes
* [ ] Add validation
* [ ] Implement concurrency-safe URL generation
* [ ] Add Redis caching
* [ ] Implement rate limiting
* [ ] Dockerize application
* [ ] Add load balancing
* [ ] Implement horizontal scaling
* [ ] Add database replication
* [ ] Explore database sharding
* [ ] Add monitoring and observability

---

## 👨‍💻 Project

**Distributed URL Shortener**

Built with:

```text
Java 17
Spring Boot
Maven
MySQL
Git
GitHub
```

The project is being developed incrementally to understand how a simple URL shortener can evolve into a scalable distributed backend system.
