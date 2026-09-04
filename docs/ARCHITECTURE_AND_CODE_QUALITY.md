# OmniBid Architecture and Code Quality

Ngày cập nhật: **04/09/2026**

Tài liệu này mô tả kiến trúc đang được code thực thi. Mục tiêu là pragmatic clean architecture cho portfolio microservices, không thêm abstraction nếu chưa có boundary cần thay thế hoặc test độc lập.

## Kết luận audit

OmniBid có cấu trúc **tốt cho một portfolio backend/fresher nâng cao**, nhưng không được mô tả là Clean Architecture hoặc production-ready tuyệt đối. Boundary giữa các microservice, database ownership, auction use case và outbound adapter đã rõ; mức độ áp dụng trong identity, wallet, audit và frontend chưa đồng đều.

| Khu vực | Đánh giá | Bằng chứng / giới hạn |
|---|---|---|
| Monorepo và service ownership | Tốt | Contract, bốn service, frontend, deploy và docs tách rõ; không có foreign key xuyên service |
| Auction application design | Tốt | Command/query use case riêng, controller mỏng, external dependency nằm sau port |
| OOP/domain invariant | Khá | Auction/wallet dùng behavior và factory; identity entity vẫn có public setter |
| SOLID/DIP | Khá | External auction adapters tuân DIP; application layer vẫn phụ thuộc trực tiếp Spring Data repository |
| Messaging correctness | Khá tốt | Outbox, broker acknowledgement và durable consumer dedupe; durable saga/reconciliation còn thiếu |
| Frontend organization | Cần cải thiện | Ba page component dài 176-292 dòng, nên tách feature hooks/components khi mở rộng |
| Architecture enforcement | Cần cải thiện | Boundary mới được giữ bằng convention/review; chưa có ArchUnit hoặc module-level architecture test |

Không áp dụng abstraction chỉ để “đủ pattern”. Repository dùng Clean Architecture theo hướng pragmatic: tách ở nơi có failure boundary hoặc cần thay adapter/test độc lập, chấp nhận JPA entity và Spring Data trong service nhỏ.

## Architectural boundaries

```text
HTTP / Scheduler
       |
       v
Application facade / focused use case
       |
       +---- Domain aggregate and invariant
       |
       +---- Outbound port
                    |
                    v
             Infrastructure adapter
```

Auction command path:

```text
AuctionController
  -> AuctionServiceImpl (thin stable facade)
      -> PlaceBidUseCase
      -> EndAuctionUseCase

AuctionController
  -> AuctionLifecycleUseCase (admin create/activate)
  -> AuctionQueryService (read projection)
```

Outbound contracts:

| Port | Adapter | Infrastructure hidden from use case |
|---|---|---|
| `AuctionLockExecutor` | `RedissonAuctionLockExecutor` | Redisson API, lock key, wait policy, safe unlock |
| `AuctionPriceCache` | `RedisAuctionPriceCache` | Redis key and best-effort failure handling |
| `WalletDepositPort` | `WalletClient` | generated protobuf classes, gRPC deadline and status mapping |

Controller không truy cập JPA repository. Generated protobuf types không đi vào application use case. Redis cache failure không làm một bid đã commit bị báo thất bại.

## OOP and domain invariants

`Auction`, `Bid`, `Wallet` và `WalletTransaction` không còn public setter tổng quát. Entity được tạo qua factory:

- `Auction.schedule(...)`
- `Bid.place(...)`
- `Wallet.open(...)`
- `WalletTransaction.succeeded(...)`

Mutation đi qua behavior có kiểm tra:

- `Auction.activate`, `validateBid`, `acceptBid`, `end`
- `Wallet.credit`, `debitAvailable`, `freeze`, `refundFrozen`

PostgreSQL vẫn là lớp bảo vệ cuối với check constraint, unique idempotency key, foreign key nội bộ service và optimistic/pessimistic locking.

## SOLID decisions

- **SRP:** place bid, end auction, lifecycle và query là use case riêng.
- **OCP/DIP:** thay Redisson, Redis cache hoặc gRPC client bằng adapter khác không sửa business workflow.
- **ISP:** mỗi outbound port chỉ chứa capability use case cần; không tạo một infrastructure facade lớn.
- **LSP:** port contract trả model trung lập (`DepositReservation`) thay vì protobuf response.
- Constructor injection được dùng cho application components; `Clock` là dependency để test thời gian xác định.

Spring Data repository được dùng trực tiếp trong application layer như một lựa chọn pragmatic. Chỉ tách thêm persistence port khi cần nhiều adapter hoặc domain module độc lập hoàn toàn khỏi Spring Data.

## Cross-service consistency

`FreezeDepositResponse.newly_created` phân biệt hai trường hợp:

1. Call hiện tại vừa tạo freeze: nếu auction transaction rollback, gọi `ReleaseDeposit`.
2. Idempotent replay hoặc user đã có deposit: không được compensation vì reservation có thể thuộc bid đã commit trước đó.

`ReleaseDeposit` xác minh original freeze transaction và ghi `REFUND` với key `refund:{transactionId}`. Vì vậy RPC retry và RabbitMQ refund sau này không hoàn tiền hai lần.

Đây là compensating transaction cho exception/commit failure quan sát được. Nếu process chết sau wallet commit nhưng trước khi chạy compensation, vẫn cần durable saga state và reconciliation job; project không tuyên bố distributed transaction exactly-once.

Refund consumer không ACK dựa trên Redis `PROCESSING` marker. Marker đó có thể còn stale sau consumer crash và làm mất bản redelivery. PostgreSQL transaction cùng unique `refund:{transactionId}` là correctness boundary; Redis chỉ cache `SUCCESS` sau DB commit và lỗi Redis không làm fail một refund đã commit.

## Public API safety

- Customer identity lấy từ JWT `sub`, không lấy từ bid body.
- `X-Idempotency-Key` là header bắt buộc cho bid.
- Bid history không trả raw bidder UUID hoặc wallet transaction ID.
- Alias bidder là SHA-256 của `(auctionId, bidderId)` rút gọn, ổn định trong một phiên nhưng khác giữa các phiên.
- Admin-only endpoints: create, activate và end auction.
- Không có automatic demo seed; wallet được provision từ `UserRegistered` và auction được tạo qua lifecycle API.

## Bounded queries

Public auction list và bid history giới hạn tối đa 100 item để tránh unbounded read. End-auction không tải toàn bộ bid history: PostgreSQL `DISTINCT ON (bidder_id)` lấy đúng một original freeze transaction cho mỗi losing participant, dùng index `(auction_id, bidder_id, placed_at)`.

## Event compatibility

`BidPlacedEvent` và `RefundCommand` có `schemaVersion=1`. Consumer tạm chấp nhận version `0` cho message/outbox row được tạo trước migration code, đồng thời reject version không hỗ trợ. Delivery vẫn là at-least-once; consumer idempotency mới là correctness boundary.

## Verification status

| Gate | Kết quả 04/09/2026 |
|---|---|
| Java compile, protobuf generation | Pass |
| Non-container backend tests | 38 pass, 0 failure/error |
| Testcontainers PostgreSQL/Redis | 4 tests chưa chạy trong phiên này vì Docker đang được giữ tắt; CI chạy bằng `mvn clean verify` |
| Frontend TypeScript | Pass |
| Next.js production build | Pass, 6 routes |
| Docker Compose parse | Pass |
| Patch whitespace | Pass |

## Remaining production work

1. Durable saga/reconciliation cho crash window sau wallet freeze.
2. Persistent signing keys/KMS và rotation thay vì key sinh trong memory khi startup.
3. Outbox exponential backoff, poison-event quarantine và backlog metrics.
4. OpenTelemetry traces, Prometheus/Grafana, structured correlation ID và SLO.
5. Broker/gRPC integration tests, load test và failure injection.
6. Tách `UserAccountService`, `WalletAccountService` và các frontend page lớn theo use case/feature khi scope tăng.
7. Chuẩn hóa event contract/schema compatibility thay vì duy trì DTO trùng giữa producer và consumer.
8. Thêm architecture test để chặn controller phụ thuộc repository/infrastructure và domain phụ thuộc application.
9. Product/catalog/media, admin UI, settlement người thắng và double-entry ledger.

Các khoảng trống này được giữ rõ ràng để CV và phỏng vấn không quảng bá quá mức trạng thái production hiện tại.
