# Centralized Logging System

## Objective

Design and implement a centralized logging system using Java microservices.

The system simulates Linux and Windows client services that generate logs, sends them to a Log Collector over TCP, forwards processed logs to a Central Log Server, and exposes APIs to query logs and view metrics.

This project demonstrates:

- Microservice communication using TCP and HTTP
- Event-driven and concurrent programming
- Resource-managed concurrency
- Reactive API design using Spring WebFlux

---

## Architecture Overview

```
Clients (Linux / Windows)
        |
        |        TCP (JSON logs)
        v
Log Collector Service
        |
        |   HTTP POST /ingest
        v
Central Log Server
```

---

## Components

### 1. Client Microservices

Client services simulate system-level logs.  
Each client runs independently and generates logs every 1–2 seconds, sending them to the Log Collector over TCP in JSON format.

**Linux Logs**
- Login audit
- Logout / syslog events

**Windows Logs**
- Login audit
- Application / system events

**Example Log Payload**
```json
{"message":"<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0)"}
```

---

### 2. Log Collector Microservice

The Log Collector listens for incoming logs over TCP and performs structured parsing.

**Responsibilities**
- Listens on TCP port `9000`
- Parses raw logs into structured fields
- Extracts timestamp, hostname, username, service, eventCategory, severity
- Checks usernames against a blacklist (`root`, `admin`)
- Uses a bounded worker pool and BlockingQueue for backpressure
- Forwards processed logs to the Central Log Server using HTTP

---

### 3. Central Log Server

Stores logs in memory and exposes REST APIs for querying and metrics.

#### Ingest API
```
POST /ingest
```

---

#### Query API
```
GET /logs
```

**Example Queries**
```bash
curl http://localhost:8082/logs
curl "http://localhost:8082/logs?username=root"
curl "http://localhost:8082/logs?isBlacklisted=true"
curl "http://localhost:8082/logs?service=linux_login&limit=10"
```

---

### 4. Metrics API

```
GET /metrics
curl 'http://localhost:8082/metrics'
```

```json
{
  "totalLogs": 120,
  "logsByCategory": {
    "login.audit": 70,
    "logout.audit": 30
  },
  "logsBySeverity": {
    "INFO": 100,
    "ERROR": 20
  }
}
```

---

## Technology Stack

- Java 17
- Spring Boot 3.x
- Spring WebFlux
- Reactor Netty
- JUnit 5, Mockito

---

## How to Run

### Central Log Server
```bash
cd log-server
mvn spring-boot:run
```

### Log Collector
```bash
cd log-collector
mvn spring-boot:run
```

### Clients
```bash
cd client-linux
java -jar target/client-linux.jar

cd client-windows
java -jar target/client-windows.jar
```

---

## Repository Structure

```
centralized-logging-system/
├── client-linux/
├── client-windows/
├── log-collector/
├── log-server/
└── README.md
```

Each service is isolated and independently runnable.
