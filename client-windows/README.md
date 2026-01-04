# Windows Log Client

## Overview

The Windows Log Client simulates Windows system and security logs and sends them to the Log Collector service over TCP.

---

## Responsibilities

- Generate Windows-style logs
- Send logs in JSON format
- Handle reconnection automatically

---

## Log Examples

```json
{"message":"<134> WIN-PC Microsoft-Windows-Security-Auditing: Account Name: admin"}
