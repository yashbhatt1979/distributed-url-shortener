# Distributed URL Shortener

A backend URL Shortener built using **Java 17, Spring Boot, Maven, and MySQL**, designed with a foundation for future distributed-system features such as Redis caching, rate limiting, horizontal scaling, database replication/sharding, and load balancing.

---

## 📅 Work Completed — September 20, 2026

Today's work focused on completing the basic **URL shortening and URL redirection flow** and testing the REST API using **Postman**.

### 1. Built `UrlController`

The `UrlController` was implemented to expose REST endpoints for the URL shortener.

Current base mapping:

```text
/shorten
```

### POST — Create Short URL

```http
POST http://localhost:8080/shorten
```

Request body:

```json
{
    "originalUrl": "https://www.google.com"
}
```

The request flows through:

```text
Postman
   ↓
UrlController
   ↓
UrlService
   ↓
ShortCodeGenerator
   ↓
UrlRepository
   ↓
MySQL
```

The service generates a short code, creates a `UrlMapping`, and stores the mapping in MySQL.

---

## 2. Added GET Redirect Endpoint

A GET endpoint was added to resolve a generated short code.

```http
GET http://localhost:8080/shorten/{shortCode}
```

Example:

```http
GET http://localhost:8080/shorten/aB3xY7
```

The request flow is:

```text
GET /shorten/aB3xY7
        ↓
UrlController
        ↓
UrlService
        ↓
UrlRepository
        ↓
MySQL
        ↓
Find original URL
        ↓
302 Found
        ↓
Location: original URL
```

The controller returns:

```text
HTTP 302 Found
```

with the original URL in the `Location` header.

Example:

```text
Location: https://www.google.com
```

---

## 3. Updated `UrlService`

The `UrlService` now supports both:

### Creating a short URL

```java
public ShortenUrlResponse shortenUrl(ShortenUrlRequest request)
```

### Resolving a short URL

```java
public String getOriginalUrl(String shortCode)
```

The GET functionality uses:

```java
urlRepository.findByShortCode(shortCode)
```

If the short code does not exist, a:

```java
UrlNotFoundException
```

is thrown.

---

## 4. Postman API Testing

### POST Request

**Method:**

```text
POST
```

**URL:**

```text
http://localhost:8080/shorten
```

**Body → raw → JSON:**

```json
{
    "originalUrl": "https://www.google.com"
}
```

This request is used to create a new short URL.

---

### GET Request

After receiving a generated short code, for example:

```text
aB3xY7
```

send:

```text
GET http://localhost:8080/shorten/aB3xY7
```

The expected behavior is:

```text
302 Found
```

with:

```text
Location: https://www.google.com
```

---

## 5. Postman Redirect Testing

While testing the GET endpoint, Postman may automatically follow the `302` redirect.

For example:

```text
GET /shorten/aB3xY7
        ↓
302 Found
        ↓
https://www.google.com
        ↓
Google HTML response
```

This can make it appear as though the API returned Google's HTML.

The application is actually working correctly.

To inspect the redirect itself:

1. Open the GET request in Postman.
2. Open the request settings.
3. Disable **Follow redirects**.
4. Send the request again.

The response should then show:

```text
302 Found
```

and the response headers should contain:

```text
Location: https://www.google.com
```

---

## 6. Current API Structure

```text
                    URL SHORTENER API
                           │
                 ┌─────────┴─────────┐
                 │                   │
                POST                GET
                 │                   │
             /shorten          /shorten/{code}
                 │                   │
                 ▼                   ▼
          Create mapping       Resolve short code
                 │                   │
                 └─────────┬─────────┘
                           │
                      UrlService
                           │
                    UrlRepository
                           │
                         MySQL
```

---

## 7. Current Application Flow

### URL Creation

```text
Client
  │
  │ POST /shorten
  │
  │ { "originalUrl": "https://www.google.com" }
  ▼
UrlController
  │
  ▼
UrlService
  │
  ├── Generate short code
  │
  ├── Create UrlMapping
  │
  └── Save mapping
          │
          ▼
     UrlRepository
          │
          ▼
        MySQL
```

### URL Redirection

```text
Client
  │
  │ GET /shorten/{shortCode}
  ▼
UrlController
  │
  ▼
UrlService
  │
  ▼
UrlRepository
  │
  ▼
MySQL
  │
  │ original URL
  ▼
UrlController
  │
  │ 302 Found
  │ Location: original URL
  ▼
Client
```

---

## 8. Key Concept Learned Today

An important distinction was established between the **endpoint URL** and the **URL contained in the request body**.

### Endpoint URL

This tells Spring Boot **which operation to execute**.

Example:

```text
POST http://localhost:8080/shorten
```

### Original URL

This is the actual data being sent to the application:

```json
{
    "originalUrl": "https://www.google.com"
}
```

The application stores the relationship:

```text
Short Code ──────────────► Original URL

aB3xY7                    https://www.google.com
```

When a user later requests:

```text
GET /shorten/aB3xY7
```

the application looks up `aB3xY7` and redirects the user to the stored original URL.

---

## 9. Status Codes Used

| Operation            | HTTP Method | Endpoint               | Expected Status |
| -------------------- | ----------- | ---------------------- | --------------- |
| Create short URL     | POST        | `/shorten`             | `201 Created`   |
| Resolve short URL    | GET         | `/shorten/{shortCode}` | `302 Found`     |
| Short code not found | GET         | `/shorten/{shortCode}` | `404 Not Found` |

---

## 10. Today's Progress

* [x] Built/updated `UrlController`
* [x] Added POST `/shorten`
* [x] Added GET `/shorten/{shortCode}`
* [x] Connected controller to `UrlService`
* [x] Implemented `getOriginalUrl()`
* [x] Connected short-code lookup to `UrlRepository`
* [x] Tested GET endpoint with Postman
* [x] Verified URL redirection behavior
* [x] Understood Postman's automatic redirect behavior
* [x] Understood the difference between API endpoint URL and original URL
* [ ] Debug/fix POST `500 Internal Server Error`
* [ ] Continue building distributed-system features

---

## 🚀 Next Step

The next development step is to investigate and fix the **500 Internal Server Error occurring during the POST `/shorten` request**.

After the basic shortening and redirection flow is stable, the project can progress toward:

```text
Redis Caching
      ↓
Rate Limiting
      ↓
Concurrency & Thread Safety
      ↓
Docker
      ↓
Load Balancing
      ↓
Horizontal Scaling
      ↓
Database Replication
      ↓
Database Sharding
      ↓
Distributed URL Shortener
```
