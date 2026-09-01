# OmniBid Project Structure

Tài liệu này là bản đồ source code của monorepo. Tổng quan kiến trúc và các quyết định distributed systems nằm ở [`PROJECT_OVERVIEW.md`](../PROJECT_OVERVIEW.md); database chi tiết nằm ở [`DATABASE_SCHEMA.md`](DATABASE_SCHEMA.md).

## Cây thư mục

```text
OmniBid/
├── contracts/
│   └── wallet-proto/                     # protobuf contract dùng chung
├── services/
│   ├── identity-service/                 # Google OIDC, user, profile, RBAC, session, JWK
│   ├── auction-service/                  # auction, bid, distributed lock, outbox
│   ├── wallet-service/                   # wallet, freeze/refund, ledger-like history
│   └── audit-service/                    # Kafka consumer, MongoDB bid audit
├── frontend/
│   └── src/
│       ├── app/                          # Next.js App Router pages
│       ├── components/                   # shared UI và auth provider
│       ├── services/                     # Axios clients và auth session client
│       └── types/                        # TypeScript contracts
├── infrastructure/postgres/init.sql      # chỉ tạo database trên Postgres container mới
├── deploy/Caddyfile                      # HTTPS edge profile tùy chọn
├── docs/                                 # architecture, schema, deployment, CV notes
├── .github/workflows/ci.yml              # backend/frontend/container CI
├── docker-compose.yml                    # backend services và local infrastructure
└── pom.xml                               # Maven reactor root
```

## Quy ước mỗi Java service

```text
src/main/java/com/omnibid/<service>/
├── config/          # Spring configuration và typed properties
├── controller/      # HTTP boundary
├── domain/          # aggregate/entity/value enum
├── dto/             # request/response contract
├── repository/      # persistence port
├── security/        # JWT/origin/authorization concern
├── service/         # application use case + outbound port contracts
├── infrastructure/  # adapters cho Redisson, Redis cache và external systems
└── messaging/       # Kafka/RabbitMQ producer hoặc consumer

src/main/resources/
├── application.yml
└── db/migration/    # Flyway là nguồn sự thật của PostgreSQL schema
```

Không phải service nào cũng cần đủ mọi package. Audit service nhỏ hơn vì nhiệm vụ chính là consume event và lưu MongoDB.

## Ownership

| Module | Sở hữu dữ liệu | Public boundary |
|---|---|---|
| `identity-service` | user, Google identity, profile, role, refresh session, identity outbox | REST `:8083`, JWK endpoint |
| `auction-service` | auction, bid, winner, auction outbox | REST `:8080`, gRPC client |
| `wallet-service` | wallet, frozen balance, transaction history | REST `:8081`, gRPC `:9091` |
| `audit-service` | immutable bid audit log | Kafka consumer, không có public API |
| `frontend` | chỉ giữ access token trong memory và UI state | Next.js `:3000` |

Mỗi service chỉ ghi database của chính nó. UUID user ở auction/wallet là external reference; không tạo foreign key xuyên database.

## Luồng đăng ký và phân quyền

1. Người dùng chọn Google trên `/login`.
2. Identity service verify chữ ký, audience, expiration, email verification và nonce.
3. Google `sub` chưa tồn tại sẽ tạo account mới với role `CUSTOMER`.
4. `UserRegistered` được ghi vào identity outbox và publish sang Kafka; wallet consumer provision ví theo `userId`.
5. Admin không được tạo từ frontend. `OMNIBID_ADMIN_EMAIL` provision account ADMIN khi identity-service khởi động.
6. Lần đầu đúng Google email đã cấu hình đăng nhập, backend liên kết Google identity vào admin account chưa có provider identity.

Không có password mặc định và không có endpoint đăng nhập mock.

## Điểm bắt đầu khi đọc code

- Authentication: `identity/controller/AuthController.java` → `UserAccountService.java` → `AuthSessionService.java`.
- Command bid: `AuctionController` → thin `AuctionServiceImpl` facade → `PlaceBidUseCase`.
- Query: `AuctionController` → `AuctionQueryService`; controller không truy cập repository trực tiếp.
- Outbound ports: `AuctionLockExecutor`, `AuctionPriceCache`, `WalletDepositPort`.
- Adapters: `RedissonAuctionLockExecutor`, `RedisAuctionPriceCache`, `grpc/WalletClient`.
- Freeze/compensation: `WalletClient` → wallet gRPC `FreezeDeposit` / `ReleaseDeposit`.
- Async audit/refund: auction outbox publisher → Kafka/RabbitMQ consumers.
- Frontend auth: `frontend/src/components/AuthProvider.tsx` và `frontend/src/services/authSession.ts`.
- Profile UI: `frontend/src/app/profile/page.tsx`.

## Build boundaries

```powershell
# Toàn bộ backend reactor
mvn clean verify

# Riêng identity và dependency cần thiết
mvn -pl services/identity-service -am test

# Frontend
Set-Location frontend
npm run typecheck
npm run build
```
