## Summary

Explain the user or system problem this pull request solves and the intended outcome.

## Type of change

- [ ] Feat — new capability
- [ ] Fix — defect correction
- [ ] Refactor — internal change without behavior change
- [ ] Perf — performance or scalability improvement
- [ ] Docs — documentation only

## Services and surfaces affected

- [ ] Identity service
- [ ] Auction service
- [ ] Wallet service
- [ ] Audit service
- [ ] Frontend
- [ ] Infrastructure / CI/CD
- [ ] Database schema or message contract

## Technical context and architecture decisions

Describe the important constraints, alternatives considered, and why this design was selected.

- **Concurrency / distributed lock:** State the protected invariant, Redis key scope, lock wait/lease behavior, and failure mode.
- **Idempotency:** State the idempotency key, retention policy, duplicate behavior, and transactional boundary.
- **gRPC:** Describe compatibility impact, timeout/error mapping, and synchronous failure handling.
- **Event-driven flow:** Describe producer, topic/queue, consumer group, retry/DLQ behavior, ordering, and delivery semantics.

Write `Not applicable` for concerns that do not apply; do not leave architectural risk implicit.

## Verification checklist

- [ ] `mvn clean verify` passes for the complete Maven reactor.
- [ ] `npm run typecheck` and `npm run build` pass in `frontend/`.
- [ ] Unit or integration tests cover the changed behavior and failure paths.
- [ ] Concurrent execution cannot violate auction or wallet invariants.
- [ ] Message retry or redelivery cannot apply a financial mutation twice.
- [ ] API, protobuf, event, and database changes are backward compatible or have a migration plan.
- [ ] Logs contain correlation identifiers and no credentials, tokens, personal data, or secrets.
- [ ] No credentials, private keys, `.env` files, or generated artifacts are committed.
- [ ] Documentation and example environment files reflect the change.

## Operational impact and rollback

Describe rollout order, observability signals, migrations, feature flags, and the safe rollback procedure.

## Evidence

Attach relevant test output, screenshots, traces, metrics, or reproducible API commands.
