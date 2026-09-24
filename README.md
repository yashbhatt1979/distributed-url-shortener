Distributed URL Shortener

Today's Work --- Horizontal Scaling, MySQL Replication & Sharding Foundation

Date: 24 September 2026

Today we worked on scaling the Distributed URL Shortener and rebuilt the
database infrastructure from a clean Docker environment.

Status: Horizontal scaling and MySQL primary-replica replication
were implemented and verified. The sharding work was prepared as the
next database-scaling stage; full production sharding was not yet
completed.

1. Clean Docker Environment

We removed the previous Docker setup so that replication could be
rebuilt from a known-good state.

Removed

Old Docker containers

Old MySQL primary data volume

Old MySQL replica data volume

Old Redis data volume

Old project Docker network

This gave us a clean database environment without stale MySQL
replication metadata.

2. Horizontal Scaling

We configured the Spring Boot application to run as multiple instances:

app-1
app-2
app-3

The instances use the same Spring Boot application image and share the
Docker network.

Architecture

                    Client
                      |
                      v
              +---------------+
              | Load Balancer  |
              +---------------+
                 /     |     \
                /      |      \
               v       v       v
            app-1    app-2    app-3
               \       |       /
                \      |      /
                 \     |     /
                  v    v    v
                MySQL Primary

The important concept is that multiple application instances can run
simultaneously, allowing the application tier to scale horizontally.

3. MySQL Primary Database

A dedicated MySQL primary container was configured:

url-shortener-mysql-primary

Configuration

MySQL 8.0

server-id=1

Binary logging enabled

Row-based binary logging

binlog-do-db=url_shortener

bind-address=0.0.0.0

Primary MySQL is exposed on:

localhost:3307

4. MySQL Replica Database

A separate MySQL replica container was configured:

url-shortener-mysql-replica

Configuration

MySQL 8.0

server-id=2

Binary logging enabled

Row-based binary logging

Relay log enabled

bind-address=0.0.0.0

Replica MySQL is exposed on:

localhost:3308

5. Replication User

The primary creates a dedicated replication user:

CREATE USER IF NOT EXISTS 'replicator'@'%'
IDENTIFIED BY 'replicator_password';

GRANT REPLICATION SLAVE, REPLICATION CLIENT
ON *.*
TO 'replicator'@'%';

We verified that the user exists and has the required privileges.

6. Primary → Replica Replication

Replication was configured manually after both MySQL servers were
healthy.

The replica was configured using:

CHANGE REPLICATION SOURCE TO
SOURCE_HOST='mysql-primary',
SOURCE_PORT=3306,
SOURCE_USER='replicator',
SOURCE_PASSWORD='replicator_password',
SOURCE_LOG_FILE='mysql-bin.000003',
SOURCE_LOG_POS=157,
GET_SOURCE_PUBLIC_KEY=1;

Replication was then started with:

START REPLICA;

Replication Flow

             MySQL Primary
             server-id = 1
                   |
                   | Binary Log
                   v
          mysql-bin.000003
                   |
                   v
             MySQL Replica
             server-id = 2
                   |
                   v
             Relay Log

7. Replication Verification

We did not rely only on SHOW REPLICA STATUS.

We tested actual database changes.

Schema replication test

A table was created on the primary:

CREATE TABLE url_shortener.replication_test (
    id INT PRIMARY KEY,
    message VARCHAR(100)
);

The table appeared automatically on the replica.

We then removed it from the primary:

DROP TABLE url_shortener.replication_test;

The deletion also propagated to the replica.

Application data replication test

A URL was created through Postman.

The resulting url_mapping row was verified on the primary:

SELECT id, short_code, original_url
FROM url_shortener.url_mapping;

The same row was then verified on the replica.

This confirmed:

Application
     |
     v
Primary MySQL
     |
     | Replication
     v
Replica MySQL

Result: MySQL data replication is working successfully.

8. Spring Boot + Flyway

The Spring Boot application was connected to the primary:

jdbc:mysql://mysql-primary:3306/url_shortener

Flyway successfully detected and applied the project's migrations.

Current application schema includes:

V1

url_mapping

with:

id

short_code

original_url

created_at

expires_at

V2

Added:

original_url_hash

with a unique constraint for concurrency protection.

9. Redis

Redis remains part of the distributed architecture:

Redis 7

It is used by the application for:

Distributed locking

Caching

Rate limiting

Redis was also checked during today's troubleshooting. The cache was
empty when we investigated the duplicate-URL behavior.

10. Duplicate URL Investigation

We also investigated a case where a URL was deleted directly from MySQL
but the application still reported that the URL already existed.

The database was checked directly:

SELECT *
FROM url_shortener.url_mapping;

The table was empty.

Redis was also checked and found empty.

After testing through Postman again, URL creation worked correctly.

This helped establish that the problem was not caused by the current
MySQL replication setup.

11. Current Docker Architecture

The current infrastructure is based around:

                    Client
                      |
                      v
             Spring Boot Apps
              /      |      \
             /       |       \
          app-1     app-2    app-3
             \       |       /
              \      |      /
               v     v     v
              MySQL Primary
                    |
                    | Replication
                    v
              MySQL Replica

                    +
                    |
                  Redis

12. Sharding

Sharding is part of the distributed database scaling plan.

The goal is to eventually distribute data across multiple database
shards rather than storing all application data on one MySQL primary.

Conceptually:

                 Application
                      |
                Shard Router
                 /    |    \
                /     |     \
               v      v      v
           Shard 1  Shard 2  Shard 3
             MySQL    MySQL    MySQL

A shard key and routing strategy still need to be finalized and
implemented.

Therefore:

Horizontal application scaling and MySQL replication were completed
and verified today. Full database sharding is the next implementation
stage.

13. What We Successfully Proved Today

Application layer

Multiple Spring Boot application instances configured

Dockerized application

Horizontal scaling architecture established

Database layer

MySQL primary configured

MySQL replica configured

Unique server IDs configured

Binary logging enabled

Replication user created

Primary → replica connection established

Schema replication tested

Data replication tested

Delete replication tested

Infrastructure

Docker network configured

Persistent MySQL volumes configured

Redis configured

Spring Boot connected to MySQL primary

Flyway migrations successfully executed

Next

Design shard key

Design shard-routing strategy

Create multiple MySQL shards

Implement application-level shard routing

Test data distribution across shards

Combine sharding with replication

Add load balancing/failover strategy

Test failure scenarios

14. Key Learning

Today's architecture introduced an important distinction:

Horizontal scaling

Adds more application instances:

app-1 + app-2 + app-3

This increases application-layer capacity.

Replication

Copies database changes:

Primary → Replica

This improves read scalability and provides a database redundancy
mechanism.

Sharding

Splits data across multiple databases:

Shard 1
Shard 2
Shard 3

This is the next major step for scaling the database itself.

15. End-of-Day Status

Distributed URL Shortener
        |
        +-- Docker -------------------- DONE
        |
        +-- Redis --------------------- DONE
        |
        +-- Horizontal Scaling -------- DONE
        |
        +-- MySQL Primary ------------- DONE
        |
        +-- MySQL Replica ------------- DONE
        |
        +-- Primary → Replica --------- VERIFIED
        |
        +-- Application Data ---------- VERIFIED
        |
        +-- Database Sharding ---------- NEXT

Today's major milestone: The project now has a clean, working
Dockerized MySQL primary-replica setup with verified application-data
replication, alongside horizontally scalable Spring Boot application
instances.