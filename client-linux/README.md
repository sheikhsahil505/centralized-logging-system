# Linux Log Client

## Overview

The Linux Log Client simulates Linux system logs and sends them to the Log Collector service over TCP.

It generates structured login and logout related logs at fixed intervals.

---

## Responsibilities

- Generate Linux-style logs (login, logout, syslog)
- Send logs in JSON format over TCP
- Reconnect automatically if the connection is lost

---

## Log Examples

```json
{"message":"<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0)"}
