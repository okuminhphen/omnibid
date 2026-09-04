# OmniBid Fresher Hardening — What Changed and Why

Ngày cập nhật: **2026-09-04**<br>
Nhánh: **`develop`**

Tài liệu này ghi lại gói cải thiện có tỷ lệ giá trị/độ phức tạp tốt nhất cho một fresher backend: đủ sâu để thảo luận distributed systems trong phỏng vấn, nhưng không thêm công nghệ chỉ để làm đẹp CV.

## Kết quả đã triển khai

### 1. Schema do Flyway quản lý

Auction và wallet không còn dùng Hibernate `ddl-auto=update`. Mỗi service có migration versioned, còn Hibernate chỉ chạy `validate` khi startup.

Điều này chứng minh ba thực hành quan trọng:

- Schema thay đổi có lịch sử và review được trong Git.
- Database rỗng được dựng lặp lại giống CI; database local cũ được baseline/migrate không cần xóa dữ liệu.
- Check constraint, foreign key, unique idempotency key và index là correctness guard ở database, không chỉ là validation Java.

### 2. Transactional Outbox cho auction

`placeBid` ghi `Bid`, cập nhật `Auction` và ghi `BID_PLACED` outbox row trong **cùng một PostgreSQL transaction**. `endAuction` cũng ghi các `REFUND_REQUESTED` row trong transaction chốt phiên.

Scheduled publisher:

1. Lock batch pending bằng `FOR UPDATE SKIP LOCKED` để nhiều instance không cùng claim một row.
2. Deserialize payload theo event type.
3. Chờ Kafka acknowledgement hoặc RabbitMQ correlated publisher confirm.
4. Chỉ sau broker ack mới ghi `published_at`.
5. Khi lỗi, giữ event pending, tăng `attempts` và lưu `last_error` để retry.

Delivery là **at-least-once**, không tuyên bố exactly-once. Consumer vẫn phải idempotent: audit dùng `bidId` làm Mongo `_id`; refund dùng `transactionId`, Redis `SUCCESS` cache và PostgreSQL unique constraint.

## 3. Distributed lock không hết lease giữa critical section

Bid/end dùng:

```java
lock.tryLock(3, TimeUnit.SECONDS)
```

Không truyền fixed lease giúp Redisson watchdog gia hạn lock khi current thread vẫn sống. Điều này loại bỏ failure mode của lease 5 giây: gRPC hoặc database chậm hơn 5 giây khiến request thứ hai lấy lock trong khi request đầu chưa xong.

`finally` vẫn chỉ unlock khi `lock.isHeldByCurrentThread()`.

## 4. Automatic auction ending

Scheduler định kỳ lấy tối đa 100 auction `ACTIVE` có `endTime <= now`, sau đó gọi lại cùng `endAuction()` dùng Redis lock. API admin và scheduler vì thế dùng chung một code path.

Refund outbox ID được tạo xác định từ wallet transaction ID. Gọi end nhiều lần không tạo command tài chính mới.

## 5. Integration và concurrency tests

Full backend hiện có **42 tests**:

| Module | Tests | Nội dung nổi bật |
| --- | ---: | --- |
| Identity | 12 | refresh rotation, reuse detection, token hashing, origin filter, cookie policy, admin bootstrap/claim, PostgreSQL schema |
| Auction | 18 | use cases, domain invariant, privacy projection, lock adapter, compensation, outbox, scheduler, PostgreSQL schema, Redis contention |
| Wallet | 11 | balance invariant, account commands, new/replayed reservation, refund idempotency, Redis failure fallback, PostgreSQL schema |
| Audit | 1 | idempotent Mongo document identity |

Testcontainers tạo PostgreSQL 16 và Redis 7.4 thật. Redis test cho 20 thread bắt đầu đồng thời và chứng minh `maxConcurrent == 1` trong critical section.

## Failure semantics có thể trình bày khi phỏng vấn

| Failure | Kết quả hiện tại |
| --- | --- |
| Redis price cache lỗi sau bid commit | Bid vẫn thành công; cache là disposable và có thể tái tạo |
| Kafka/RabbitMQ tạm unavailable | Outbox row còn pending và được poll lại |
| Publisher crash sau broker ack, trước `published_at` | Message có thể redeliver; consumer idempotency ngăn side effect trùng |
| Hai auction-service cùng poll outbox | `SKIP LOCKED` ngăn cùng claim row trong một thời điểm |
| Bid request chạy lâu hơn 5 giây | Watchdog gia hạn distributed lock |
| Scheduler và admin cùng end auction | Cùng Redis lock; refund command ID xác định và durable dedupe |
| Redis mất idempotency refund key | PostgreSQL unique transaction vẫn là safety net bền vững |
| Consumer crash sau khi nhận refund | Không dùng `PROCESSING` marker để ACK; redelivery vẫn đi qua PostgreSQL durable idempotency |
| Redis lỗi khi đọc/ghi refund cache | Refund vẫn xử lý bằng PostgreSQL; lỗi cache không làm fail một DB commit đã thành công |
| Wallet freeze mới thành công nhưng auction DB rollback | `ReleaseDeposit` compensation dùng original transaction ID; crash trước compensation vẫn cần reconciliation |

## Lệnh kiểm chứng

Docker Desktop phải chạy vì backend integration tests dùng Testcontainers.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn --batch-mode --no-transfer-progress clean verify

Set-Location frontend
npm run typecheck
npm run build
```

Kết quả đã xác minh trong đợt hardening này:

- Maven reactor: 6/6 project thành công.
- Backend ngày 04/09: 38 non-container tests pass; 4 Testcontainers tests cần Docker Desktop hoặc GitHub Actions.
- Frontend typecheck: pass.
- Next.js production build: pass, 6 routes.
- Docker infrastructure: 6/6 container healthy.

## Demo nên quay hoặc trình bày

1. Mở Customer A và Customer B ở hai browser context.
2. Nạp tiền demo cho cả hai.
3. Gửi bid gần đồng thời và chỉ ra Redis lock + optimistic version + unique idempotency key.
4. Dừng Redpanda, đặt bid, rồi query `auction_outbox_events` để thấy event pending.
5. Bật Redpanda lại và chứng minh publisher gửi event, Audit Mongo có đúng một `bidId`.
6. Đặt `endTime` qua thời điểm hiện tại hoặc gọi API end, rồi quan sát refund command đi qua RabbitMQ và wallet transaction không trùng.

## Nên làm tiếp theo cho CV

Ưu tiên theo thứ tự:

1. Product/catalog + admin CRUD + S3 presigned upload vì đây là khoảng trống nghiệp vụ dễ nhìn thấy nhất.
2. k6/Gatling load test kèm báo cáo throughput, p95/p99, lock timeout và success rate.
3. OpenTelemetry + Prometheus/Grafana cho HTTP/gRPC latency, error rate và outbox backlog.
4. Durable saga state + reconciliation để bổ sung cho compensating RPC khi process crash/network partition.
5. SSE/WebSocket để thay polling 1,5 giây khi cần real-time fan-out thật.
6. Double-entry ledger và winner settlement sau khi có seller ownership model.

**Chưa cần Elasticsearch lúc này.** Hệ thống chưa có catalog/search use case đủ mạnh; thêm Elasticsearch chỉ tăng RAM, operational burden và consistency problem. Chỉ thêm khi có yêu cầu rõ như full-text product search, filter/facet, typo tolerance và đo được PostgreSQL search không còn đáp ứng.

## Cách mô tả trung thực trong CV

> Hardened a Java 21 auction microservices platform with Flyway-owned schemas, a PostgreSQL transactional outbox for Kafka/RabbitMQ delivery, Redisson watchdog-based distributed locking, automatic auction expiration, and Testcontainers integration tests validating database idempotency and 20-thread Redis lock contention.

Không gọi hệ thống là production fintech hoặc exactly-once. Điểm mạnh trong phỏng vấn là giải thích được invariant, failure window, trade-off và bước tiếp theo.
