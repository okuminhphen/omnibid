# OmniBid repository audit

Ngày hoàn tất kiểm tra: **2026-08-20**

Nhánh kiểm tra: **`develop`**

Phạm vi: lịch sử Git, backend reactor, frontend production build, Docker Compose, secret hygiene, tài liệu GitHub và tài nguyên Docker cũ.

## Kết luận

Repository hiện đạt trạng thái có thể mở pull request: mã nguồn build được bằng Java 21 và Node.js 20-compatible toolchain, toàn bộ test hiện có đều qua, frontend tạo được production bundle, Docker Compose hợp lệ, không có build artifact hoặc chữ ký secret phổ biến bị track. Các thay đổi lớn đã được chia thành commit Conventional Commits có phạm vi rõ ràng thay vì một commit tổng hợp.

Badge CI trên README chỉ chuyển sang trạng thái thực sau khi nhánh `develop` được push và workflow GitHub Actions chạy thành công trên GitHub.

## Verification matrix

| Gate | Lệnh / kiểm tra | Kết quả |
| --- | --- | --- |
| Backend toolchain | JDK 21.0.1, Maven 3.9.12 | Pass |
| Backend reactor | `mvn --batch-mode --no-transfer-progress clean verify` | Pass, 6/6 reactor projects |
| Backend tests | Identity 6, Auction 10, Wallet 7, Audit 1 | Pass, **24/24**, 0 failure/error/skipped |
| PostgreSQL integration | Auction/wallet Flyway migrations và unique idempotency constraints trên PostgreSQL 16 Testcontainers | Pass |
| Redis concurrency | 20 contenders dùng Redisson lock trên Redis 7.4 Testcontainers | Pass, max critical-section concurrency = 1 |
| Frontend toolchain | Node.js 20.20.2 | Pass |
| Dependency install | `npm ci` | Pass, 132 packages audited, **0 vulnerability** |
| TypeScript | `npm run typecheck` | Pass |
| Next.js production | `npm run build` | Pass, 6 routes generated |
| Docker Compose | `docker compose config --quiet` | Pass |
| Patch hygiene | `git diff --check` | Pass |
| Tracked artifacts | `target/`, `node_modules/`, `.next/`, `.class`, `.jar` | Không có artifact bị track |
| Secret signatures | Private-key headers, GitHub/OpenAI/Google key patterns | Không phát hiện trong tracked files |
| Code markers | `TODO`, `FIXME`, `HACK` trong Java/TS/YAML/SQL | Không phát hiện |

## Sạn đã phát hiện và xử lý

### Protobuf build trên Windows/OneDrive

Plugin `org.xolstice:protobuf-maven-plugin:0.6.1` đã ngừng bảo trì và thỉnh thoảng thất bại khi dọn dependency proto tạm trên Windows. Build đã được chuyển sang `io.github.ascopes:protobuf-maven-plugin:5.1.7`; Java messages và gRPC stubs được sinh bởi goal `generate`. Sau thay đổi, `clean verify` chạy thành công trên toàn bộ reactor.

### CI và repository governance

- Backend và frontend có job độc lập, timeout, dependency cache, read-only repository permission và concurrency cancellation.
- Pull request template buộc ghi rõ distributed lock, idempotency, gRPC, delivery semantics, rollback và verification.
- Feature/bug templates có architecture trade-offs, service impact, logs và điều kiện phân tán.
- MIT license, release notes `v1.0.0`, About text và GitHub topics đã được chuẩn hóa.

### Next.js generated type hygiene

Next.js 16 tự tái tạo `next-env.d.ts` với đường dẫn khác nhau giữa `dev` và production build. File này đã được bỏ khỏi Git và thêm vào `.gitignore`; script `typecheck` chạy `next typegen` trước `tsc --noEmit`. Quy trình đã được kiểm tra lại sau khi xóa hoàn toàn `frontend/.next`, nên CI không phụ thuộc artifact sinh từ máy developer.

### Database schema ownership

- Auction và wallet chuyển từ Hibernate `ddl-auto=update` sang `ddl-auto=validate`.
- Flyway migration tạo schema, check/unique/foreign-key constraints và query indexes theo cách tương thích cả database rỗng lẫn volume local cũ.
- Testcontainers khởi tạo PostgreSQL sạch trong CI để phát hiện migration sai thứ tự hoặc entity/schema drift.

### Auction messaging reliability

- Bid và refund command được ghi vào `auction_outbox_events` trong cùng transaction với aggregate update.
- Publisher dùng `FOR UPDATE SKIP LOCKED`, Kafka acknowledgement và RabbitMQ correlated publisher confirm trước khi đánh dấu `published_at`.
- Failed publication giữ row pending, tăng `attempts` và lưu `last_error` để retry/quan sát.
- Automatic end scheduler dùng lại distributed lock của `endAuction`; deterministic refund outbox ID làm thao tác lặp lại an toàn.
- Redisson dùng watchdog renewal thay cho fixed 5-second lease, tránh hai writer cùng vào critical section khi gRPC/DB chậm.

### Docker ChatbotX cleanup

Đã xác minh bằng Compose label trước khi xóa để không dùng name matching mơ hồ.

| Resource | Trạng thái |
| --- | --- |
| 7 container `chatbotx-*` | Đã xóa |
| Network `chatbotx_chatbotx-network` | Đã xóa |
| 7 image chỉ được ChatbotX sử dụng | Đã xóa |
| 4 volume `chatbotx_cache-data`, `chatbotx_db-data`, `chatbotx_filesystem-data`, `chatbotx_redis-ui-data` | **Đang giữ an toàn; cần xác nhận riêng trước khi xóa dữ liệu vĩnh viễn** |
| Resource `omnibid-*` | Không bị xóa hoặc đổi cấu hình |

## Rủi ro còn lại trước production thật

Đây là các giới hạn được công khai để reviewer phân biệt rõ “production-style portfolio” với production deployment thực tế:

1. Testcontainers hiện bao phủ PostgreSQL và Redis; CI chưa có broker/gRPC integration test cho Kafka, RabbitMQ, MongoDB và gRPC network boundary.
2. Chưa có automated load test để chứng minh throughput, lock contention, p95/p99 latency và behavior khi Redis failover.
3. Transactional outbox đã áp dụng cho identity và auction; publisher vẫn là scheduled polling và chưa có exponential backoff, poison-event quarantine hay CDC.
4. Redis idempotency là lớp chống duplicate nhanh nhưng chưa thay thế unique constraint/transaction ledger bền vững cho mọi financial command.
5. FreezeDeposit gRPC thành công trước khi auction transaction commit vẫn cần compensation/reconciliation nếu DB commit thất bại.
6. Admin đã có role boundary và quyền kết thúc auction, nhưng product/catalog CRUD, moderation, media upload và admin dashboard đầy đủ vẫn là roadmap.
7. Google One Tap cần một Google Web Client ID thực và HTTPS origin khi deploy; dev login phải tắt ngoài local profile.
8. Chưa có Kubernetes manifests, secret manager, TLS/mTLS, OpenTelemetry collector, metrics/alerts, SLO, backup/restore drill hoặc disaster-recovery runbook.
9. Ví chỉ mô phỏng internal credits, không kết nối ngân hàng, payment gateway, KYC/AML hoặc sổ cái kế toán kép.

Các giới hạn này không chặn việc dùng repository làm portfolio. Chúng là các hướng mở rộng có giá trị để thảo luận trong phỏng vấn và tránh tuyên bố quá mức trong CV.

## Trình tự đề xuất tiếp theo

1. Push `develop`, mở PR vào `main`, chờ cả `backend-ci` và `frontend-ci` xanh.
2. Bật branch protection và bắt buộc hai status checks cùng một approval.
3. Merge bằng squash hoặc merge commit theo policy của repository; không force-push `main`.
4. Tag/release `v1.0.0` theo `docs/GITHUB_RELEASE_GUIDE.md` sau khi CI trên `main` thành công.
5. Phase tiếp theo nên ưu tiên product/admin lifecycle hoặc observability + load/failure testing; chưa cần Elasticsearch khi chưa có search/catalog use case và dữ liệu đủ lớn.
