
# Log Collector Service

## Overview

The Log Collector receives logs from client services over TCP, parses and enriches them, and forwards structured logs to the Central Log Server.

---

## Responsibilities

- Accept logs over TCP
- Parse raw log messages
- Extract metadata (hostname, username, service, category, severity)
- Mark logs as blacklisted if applicable
- Forward logs using HTTP
- Process logs concurrently using bounded workers

---

## Architecture

- TCP Server (Reactor Netty)
- BlockingQueue for buffering
- Fixed worker pool
- WebClient for forwarding logs

---

## Blacklist

The following usernames are treated as blacklisted:
- root
- admin

---

## Endpoints Used

| Method | Endpoint |
|------|---------|
| POST | http://localhost:8082/ingest |

---

## Technology Stack

- Java 17
- Spring Boot 3
- Spring WebFlux
- Reactor Netty
- WebClient
- ExecutorService

---

## How to Run

```bash
mvn clean package
mvn spring-boot:run
