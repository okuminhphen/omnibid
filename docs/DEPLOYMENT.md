# OmniBid Deployment Guide

OmniBid dùng hai deployment surface độc lập:

- **Frontend:** Next.js deploy trên Vercel; không build Docker image.
- **Backend:** Identity, Auction, Wallet, Audit và infrastructure chạy bằng Docker Compose trên một Linux host/VPS.

## 1. Full backend local bằng Docker Compose

Yêu cầu: Docker Desktop/Engine với Compose v2 và tối thiểu khoảng 6 GB RAM khả dụng.

```powershell
Copy-Item .env.example .env
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Compose chờ PostgreSQL, MongoDB, Redis, RabbitMQ và Redpanda healthy trước khi khởi động application phụ thuộc. Application containers chạy bằng UID/GID `10001`, read-only root filesystem, `/tmp` tmpfs và `no-new-privileges`.

Endpoint local:

| Service | Endpoint |
| --- | --- |
| Identity | `http://localhost:8083/actuator/health` |
| Auction | `http://localhost:8080/actuator/health` |
| Wallet | `http://localhost:8081/actuator/health` |
| Wallet gRPC | chỉ expose trong Docker network tại `wallet-service:9091` |
| Audit | headless Kafka consumer; kiểm tra `docker compose logs audit-service` |
| RabbitMQ UI | `http://localhost:15672` |

Để chỉ chạy infrastructure và phát triển backend bằng Maven:

```powershell
docker compose up -d postgres mongodb redis rabbitmq redpanda localstack
```

## 2. Cấu hình backend trên VPS

Copy file mẫu rồi thay toàn bộ example credential/domain:

```bash
cp deploy/.env.backend.example .env
chmod 600 .env
```

Các giá trị bắt buộc phải kiểm tra:

```dotenv
POSTGRES_PASSWORD=<random-password-1>
RABBITMQ_DEFAULT_PASS=<random-password-2>
IDENTITY_ACTIVE_PROFILE=production
OMNIBID_DOMAIN=example.com
ACME_EMAIL=admin@example.com
OMNIBID_TOKEN_ISSUER=https://identity.example.com
FRONTEND_ORIGINS=https://app.example.com
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
```

`IDENTITY_ACTIVE_PROFILE=production` là bắt buộc trên host public: profile này làm `DevAuthController` không được tạo, nên các alias `customer-a`, `customer-b`, `admin` chỉ tồn tại trong local demo.

Tạo ba DNS `A/AAAA` record trỏ về VPS:

```text
identity.example.com
auction.example.com
wallet.example.com
```

Sau khi DNS cập nhật, chạy Caddy edge profile:

```bash
docker compose --profile edge up -d --build
docker compose ps
docker compose logs --tail=100 gateway
```

Caddy tự lấy/chuyển hạn TLS certificate. Chỉ public TCP `80/443` và SSH trên firewall. PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ và LocalStack bind vào `127.0.0.1`, không public ra Internet.

## 3. Deploy frontend lên Vercel Free

Trong Vercel Dashboard:

1. Import repository `okuminhphen/omnibid`.
2. Chọn **Root Directory = `frontend`**.
3. Framework Preset để **Next.js**; `frontend/vercel.json` dùng `npm ci` và `npm run build`.
4. Thêm các environment variables cho Production và Preview nếu cần:

```dotenv
NEXT_PUBLIC_IDENTITY_API_URL=https://identity.example.com
NEXT_PUBLIC_AUCTION_API_URL=https://auction.example.com
NEXT_PUBLIC_WALLET_API_URL=https://wallet.example.com
```

5. Deploy và gắn custom domain `app.example.com` nếu có.

Vercel hỗ trợ chọn Root Directory cho project trong monorepo và quản lý environment variables tại Project Settings. `NEXT_PUBLIC_*` được đóng vào client bundle lúc build, vì vậy đổi URL backend cần redeploy frontend.

### Cookie giữa Vercel và backend

Khuyến nghị dùng `app.example.com` trên Vercel và các backend subdomain cùng `example.com`. Khi đó:

```dotenv
FRONTEND_ORIGINS=https://app.example.com
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
```

Nếu giữ domain `*.vercel.app` trong khi backend ở `*.example.com`, request là cross-site. Cấu hình backend:

```dotenv
FRONTEND_ORIGINS=https://your-project.vercel.app
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
```

Một số browser/user policy chặn third-party cookie dù đã dùng `SameSite=None`; custom domain cùng site là phương án ổn định hơn.

## 4. Google One Tap production

Trong Google Cloud OAuth Web Client:

- Thêm frontend URL vào Authorized JavaScript origins.
- Dùng cùng client ID cho `GOOGLE_CLIENT_ID` trên identity-service.
- Đặt `GOOGLE_AUTH_ENABLED=true`.

Không có Google client secret trong One Tap frontend flow và không đưa secret vào Vercel `NEXT_PUBLIC_*`.

## 5. Rollout và rollback

Trước rollout:

```bash
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
```

Tag image/repository theo release thay vì chỉ dùng `local`:

```dotenv
OMNIBID_IMAGE_TAG=v1.1.0
```

Rollback code bằng Git tag/commit trước, rebuild cùng tag cũ rồi `docker compose up -d`. Không rollback Flyway bằng cách xóa schema; tạo forward migration sửa lỗi. Backup PostgreSQL/MongoDB volume trước migration có rủi ro.

## 6. Giới hạn production còn lại

Compose này là single-host deployment package, chưa phải high availability:

- Identity signing key hiện sinh lại khi restart; production dài hạn cần persistent PEM/KMS/Vault và key rotation.
- Database/broker vẫn là single node; cần managed service/replication và backup drill cho production thật.
- Chưa có centralized secret manager, OpenTelemetry collector, metrics/alerts hoặc automated zero-downtime rollout.
- Không scale nhiều identity replica cho đến khi signing key được chia sẻ an toàn.

Các giới hạn được giữ rõ để không quảng bá portfolio project thành một fintech production system.
