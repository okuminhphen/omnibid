---
name: Bug report
about: Report a reproducible OmniBid defect
title: "fix: "
labels: bug
assignees: ""
---

## Summary

Describe what failed and its user or system impact.

## Service affected

- [ ] Identity service
- [ ] Auction service
- [ ] Wallet service
- [ ] Audit service
- [ ] Frontend
- [ ] PostgreSQL / MongoDB / Redis
- [ ] Kafka / RabbitMQ / gRPC
- [ ] Docker / CI/CD

## Environment

- Commit or release:
- Operating system:
- Browser or API client:
- Deployment mode: local Docker / CI / other

## Steps to reproduce

1. 
2. 
3. 

## Expected behavior

Describe the correct result and any invariant that must hold.

## Actual behavior

Describe the observed result, status codes, timing, frequency, and whether retry changes the outcome.

## Logs and correlation data

Paste sanitized logs, stack traces, trace IDs, message IDs, transaction IDs, or screenshots. Remove access tokens, cookies, credentials, and personal data.

```text
sanitized diagnostic output
```

## Distributed-systems conditions

- Was the request concurrent or retried?
- Was a lock timeout, optimistic-lock conflict, duplicate message, out-of-order event, or consumer restart involved?
- Were Kafka, RabbitMQ, Redis, gRPC, or a database unavailable or degraded?

## Regression and severity

- Last known working commit or release:
- Workaround:
- Data corruption or financial impact:
- Reproducibility: always / intermittent / once

## Suggested verification

Describe the smallest automated test that would fail before the fix and pass afterward.
