# GitHub delivery and release guide

Tài liệu này là checklist phát hành cho repository `okuminhphen/omnibid`. Các lệnh được chạy tại root của monorepo sau khi CI local đã thành công.

## 1. Push `develop` và mở pull request

```bash
git status
git log --oneline main..develop
git push -u origin develop
gh pr create \
  --repo okuminhphen/omnibid \
  --base main \
  --head develop \
  --title "feat: deliver OmniBid identity, secure wallet, CI, and repository standards" \
  --body-file .github/PULL_REQUEST_TEMPLATE.md
```

Nếu chưa cài GitHub CLI, push bằng lệnh trên rồi mở:

`https://github.com/okuminhphen/omnibid/compare/main...develop?expand=1`

Không merge khi hai job `backend-ci` và `frontend-ci` chưa thành công. Khuyến nghị bật branch protection cho `main`: bắt buộc pull request, ít nhất một approval, dismiss stale approvals, require conversation resolution, và require hai status checks này.

## 2. Release `v1.0.0`

Sau khi PR đã merge vào `main`:

```bash
git checkout main
git pull --ff-only origin main
git tag -a v1.0.0 -m "OmniBid v1.0.0"
git push origin v1.0.0
gh release create v1.0.0 \
  --repo okuminhphen/omnibid \
  --title "OmniBid v1.0.0 — Distributed Auction Platform" \
  --notes-file docs/releases/v1.0.0.md
```

### Release notes

```markdown
# OmniBid v1.0.0 — Distributed Auction Platform

OmniBid v1.0.0 is the first portfolio-grade release of a real-time auction monorepo focused on correctness under concurrency, financial idempotency, and observable event-driven workflows.

## Changelog

- Added an Identity Service with Google One Tap/OIDC login, RBAC, short-lived RS256 access tokens, opaque rotating refresh sessions, reuse detection, and an outbox-backed registration event.
- Secured auction and wallet APIs with authenticated user context; bid and wallet ownership are no longer accepted from client-supplied identity fields.
- Added customer-scoped wallet balance, top-up, withdrawal, and transaction-history endpoints with database migrations.
- Implemented Redis/Redisson per-auction distributed locking, validation under lock, PostgreSQL optimistic-lock fallback, gRPC deposit freezing, and Kafka bid publication.
- Implemented RabbitMQ refund commands with retry/DLQ topology and Redis-backed duplicate protection for financial mutations.
- Added Kafka-to-MongoDB immutable bid audit logging and automatic wallet provisioning from identity events.
- Added a Next.js 16 and TypeScript frontend for authentication, profile management, wallet operations, live auction polling, and authenticated bidding.
- Added reproducible Docker infrastructure, architecture documentation, pull request/issue standards, and separate backend/frontend CI gates.

## Key architectural wins

- **Concurrency correctness:** one Redis lock namespace per auction serializes competing price transitions; state is re-read and validated inside the critical section.
- **Defense in depth:** JPA optimistic versioning protects database writes if application-level coordination is bypassed.
- **Financial idempotency:** stable transaction identifiers plus Redis deduplication prevent duplicate refund delivery from applying twice.
- **Clear consistency boundaries:** gRPC is used where the bid requires an immediate deposit decision; Kafka and RabbitMQ carry durable asynchronous side effects.
- **Identity without server-side access-token state:** short-lived signed access tokens remain stateless, while revocable refresh-session state is persisted and rotated securely.
- **Auditable data flow:** bid events are retained independently in MongoDB, and message consumers have explicit group/queue ownership.

## Verification

- Maven reactor: `mvn clean verify`
- Frontend: `npm ci && npm run typecheck && npm run build`
- Local infrastructure: PostgreSQL 16, MongoDB 7, Redis 7.4, Redpanda, RabbitMQ 3.13, and LocalStack

See `README.md`, `PROJECT_OVERVIEW.md`, and `docs/PHASE_4_IDENTITY_AND_MARKETPLACE_DESIGN.md` for architecture and local-run instructions.
```

## 3. GitHub About

Description:

> Production-style real-time auction monorepo demonstrating distributed locking, financial idempotency, gRPC, Kafka, RabbitMQ, OIDC/RBAC, and event-driven microservices with Java 21 and Next.js.

Website:

> Leave empty until a public deployment is available; do not point recruiters to `localhost`.

Topics:

```text
java spring-boot distributed-systems microservices real-time-auction redis redisson grpc kafka rabbitmq postgresql mongodb nextjs typescript event-driven-architecture idempotency oauth2 jwt docker monorepo
```

Apply the metadata with GitHub CLI:

```bash
gh repo edit okuminhphen/omnibid \
  --description "Production-style real-time auction monorepo demonstrating distributed locking, financial idempotency, gRPC, Kafka, RabbitMQ, OIDC/RBAC, and event-driven microservices with Java 21 and Next.js." \
  --add-topic java \
  --add-topic spring-boot \
  --add-topic distributed-systems \
  --add-topic microservices \
  --add-topic real-time-auction \
  --add-topic redis \
  --add-topic redisson \
  --add-topic grpc \
  --add-topic kafka \
  --add-topic rabbitmq \
  --add-topic postgresql \
  --add-topic mongodb \
  --add-topic nextjs \
  --add-topic typescript \
  --add-topic event-driven-architecture \
  --add-topic idempotency \
  --add-topic oauth2 \
  --add-topic jwt \
  --add-topic docker \
  --add-topic monorepo
```
