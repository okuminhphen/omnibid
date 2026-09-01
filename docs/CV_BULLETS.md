# OmniBid CV Bullet Points

Chỉ dùng số liệu đã được repository/CI kiểm chứng. Không tự thêm throughput hoặc latency nếu chưa chạy load test.

## English — concise version

- Built a near-real-time auction platform as a Java 21/Spring Boot microservices monorepo with Next.js, PostgreSQL, Redis, Kafka/Redpanda, RabbitMQ, MongoDB, and gRPC.
- Designed concurrency-safe bidding using Redisson distributed locks with watchdog renewal, JPA optimistic locking, PostgreSQL row locking, and durable idempotency constraints.
- Implemented transactional outbox delivery for Kafka bid events and RabbitMQ refund commands with broker acknowledgements and idempotent consumers.
- Developed Google OIDC authentication, RS256 JWT access tokens, rotating HttpOnly refresh sessions, token-reuse detection, RBAC, and user-scoped wallet APIs.
- Containerized four backend services with multi-stage builds, non-root/read-only runtime containers, health-gated Docker Compose startup, and optional Caddy automatic HTTPS.
- Added GitHub Actions CI and 26 backend tests, including Testcontainers PostgreSQL migration/idempotency tests and a 20-thread Redis lock contention test; validated Next.js typecheck and production builds.

## Tiếng Việt

- Xây dựng sàn đấu giá near real-time dạng microservices bằng Java 21/Spring Boot và Next.js, tích hợp PostgreSQL, Redis, Kafka/Redpanda, RabbitMQ, MongoDB và gRPC.
- Thiết kế luồng bid chống race condition bằng Redisson distributed lock có watchdog, JPA optimistic lock, PostgreSQL row lock và unique idempotency constraint.
- Triển khai Transactional Outbox cho Kafka bid event và RabbitMQ refund command, chờ broker acknowledgement và xử lý consumer idempotent.
- Xây dựng Google OIDC, JWT RS256, rotating HttpOnly refresh session, phát hiện token reuse, RBAC và API ví theo ownership.
- Đóng gói bốn backend service bằng multi-stage Docker build, non-root/read-only container, health-gated Compose và Caddy HTTPS tùy chọn.
- Thiết lập GitHub Actions CI với 26 backend tests, Testcontainers PostgreSQL/Redis và frontend production build.

## Interview talking points

1. Vì sao Redis lock không phải correctness guard duy nhất và vẫn cần `@Version`, database transaction, row lock, unique constraint.
2. Vì sao outbox là at-least-once và consumer vẫn phải idempotent.
3. Compensating `ReleaseDeposit` xử lý rollback thông thường sau freeze; failure window do process crash còn lại cần durable saga/reconciliation.
4. Lý do Kafka dùng cho event stream/audit còn RabbitMQ dùng cho command/retry/DLQ.
5. Trade-off của polling UI, scheduled outbox polling và kế hoạch SSE/CDC khi scale.
6. Vì sao không thêm Elasticsearch khi chưa có catalog search use case và đo lường cần thiết.
