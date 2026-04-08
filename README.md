# Mini Banking Transfer System

Hệ thống mô phỏng quy trình chuyển tiền nội bộ theo kiến trúc microservices. Dự án tập trung vào các vấn đề kỹ thuật thường gặp trong hệ phân tán: xác thực qua gateway, orchestration saga, idempotency, outbox pattern, event-driven communication và observability.

## 1. Mục tiêu dự án

Phạm vi nghiệp vụ chính:
- đăng ký và đăng nhập người dùng
- chuyển tiền giữa hai tài khoản trong cùng hệ thống
- kiểm tra gian lận trước khi xử lý giao dịch
- ghi nhận trạng thái giao dịch và cơ chế bù trừ khi có lỗi
- tạo notification cho người gửi và người nhận
- lưu audit trail cho các transfer event
- theo dõi metrics, traces và logs của toàn hệ thống

## 2. Kiến trúc tổng quan

Các thành phần chính:
- `frontend/`: giao diện web tĩnh dùng HTML/CSS/JavaScript
- `infra/nginx`: serve frontend và reverse proxy cho `/api/*`
- `infra/kong`: API Gateway cho `auth` và `transfer`, kiểm tra JWT cho API chuyển tiền
- `services/auth-service`: đăng ký, đăng nhập, sinh JWT, đồng bộ consumer/JWT credential vào Kong
- `services/account-service`: tạo tài khoản, tra cứu tài khoản, debit, credit, compensate
- `services/fraud-detection-service`: kiểm tra các luật chống gian lận đơn giản
- `services/transaction-service`: điều phối saga chuyển tiền, idempotency, outbox, Kafka publish
- `services/notification-service`: consume transfer event và tạo thông báo incoming/outgoing
- `services/audit-service`: consume transfer event và lưu audit record
- `PostgreSQL`: lưu dữ liệu nghiệp vụ
- `Redis`: lưu `Idempotency-Key`
- `Kafka`: truyền transfer event giữa transaction-service và các consumer
- `Prometheus`, `Grafana`, `Jaeger`, `OpenTelemetry Collector`, `Elasticsearch`, `Kibana`, `Filebeat`: stack observability

### Routing thực tế

- `Frontend -> Nginx -> Kong -> auth-service`
- `Frontend -> Nginx -> Kong -> transaction-service`
- `Frontend -> Nginx -> account-service`
- `Frontend -> Nginx -> notification-service`

Nghĩa là:
- `auth` và `transfer` đi qua Kong
- `account` và `notification` được Nginx route trực tiếp

## 3. Các pattern đã áp dụng

- `API Gateway`: Kong được dùng để route auth/transfer và kiểm tra JWT cho API chuyển tiền.
- `Saga`: `transaction-service` điều phối các bước fraud check, debit, credit và compensate.
- `Outbox Pattern`: trạng thái giao dịch và event được ghi vào DB trước khi publish Kafka.
- `Event-driven`: notification-service và audit-service consume cùng một transfer event từ Kafka.
- `Idempotency`: Redis lưu `Idempotency-Key` để chặn retry cùng request.
- `Circuit Breaker`: Resilience4j bảo vệ lời gọi từ transaction-service sang fraud-service và account-service.
- `Database per Service`: mỗi service sở hữu dữ liệu riêng, không join DB chéo service.
- `Observability`: actuator, Prometheus metrics, OTLP tracing, correlated logging.

## 4. Observability đã có trong code

Không chỉ dừng ở hạ tầng, observability hiện đã được tích hợp vào service code:
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`
- `@Observed` trên các use case chính
- custom metrics bằng `MeterRegistry`
- log pattern chứa `traceId`, `spanId`, `requestId`

Một số metric nghiệp vụ hiện có:
- `banking.auth.register.success`
- `banking.auth.login.success`
- `banking.account.create.success`
- `banking.account.debit.success`
- `banking.fraud.decisions`
- `banking.transfer.requests`
- `banking.transfer.duration`
- `banking.notification.events`
- `banking.audit.events`

## 5. Cấu trúc repository

```text
mini-banking-transfer-system/
├── frontend/                 # UI web
├── infra/                    # docker-compose, nginx, kong, kafka, otel, prometheus, grafana...
├── scripts/                  # script chạy service, seed dữ liệu, smoke test
├── services/
│   ├── auth-service/
│   ├── account-service/
│   ├── fraud-detection-service/
│   ├── transaction-service/
│   ├── notification-service/
│   └── audit-service/
├── docs/
│   ├── analysis-and-design-ddd.md
│   ├── architecture.md
│   └── api-specs/
└── pom.xml
```

## 6. Cách chạy dự án

### 6.1 Yêu cầu môi trường

- Java 21
- Maven
- Docker Desktop / Docker Engine
- Docker Compose

### 6.2 Chạy hạ tầng dùng profile `core`

Dùng khi cần chạy demo nghiệp vụ chính với cấu hình gọn hơn.

```powershell
docker compose -f infra/docker-compose.yml --profile core up -d
```

Profile `core` khởi động:
- `frontend` (Nginx)
- `kong`
- `zookeeper`
- `kafka`
- `kafka-init`
- `redis`
- `postgres`

### 6.3 Chạy hạ tầng dùng profile `full`

Dùng khi cần demo observability đầy đủ.

```powershell
docker compose -f infra/docker-compose.yml --profile full up -d
```

Profile `full` bổ sung thêm:
- `jaeger`
- `otel-collector`
- `prometheus`
- `grafana`
- `elasticsearch`
- `kibana`
- `filebeat`

### 6.4 Chạy backend service

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-services.ps1
```

Hoặc chạy từng service riêng:

```powershell
mvn -q -pl services/auth-service spring-boot:run
mvn -q -pl services/account-service spring-boot:run
mvn -q -pl services/fraud-detection-service spring-boot:run
mvn -q -pl services/transaction-service spring-boot:run
mvn -q -pl services/notification-service spring-boot:run
mvn -q -pl services/audit-service spring-boot:run
```

Linux:

```bash
./scripts/start-services.sh
```

### 6.5 Seed dữ liệu demo

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1
```

Linux:

```bash
./scripts/seed-demo-data.sh
```

### 6.6 Smoke test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1
```

## 7. Demo flow

1. Khởi động `core` hoặc `full` stack.
2. Chạy toàn bộ Spring Boot services.
3. Seed dữ liệu demo.
4. Mở frontend tại `http://localhost/`.
5. Đăng nhập bằng tài khoản demo.
6. Thực hiện một giao dịch chuyển tiền.
7. Kiểm tra số dư, trạng thái giao dịch và notification ở tài khoản liên quan.
8. Nếu chạy `full`, kiểm tra metrics, traces và logs.

### Tài khoản demo

- `alice / secret123`
- `bob / secret123`

Tài khoản minh họa thường dùng:
- `alice -> 100001`
- `bob -> 200001`

## 8. Các endpoint nghiệp vụ chính

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/accounts/{accountNumber}`
- `GET /api/accounts/by-owner/{ownerName}`
- `POST /api/accounts`
- `POST /api/transfers`
- `GET /api/transfers/{transferId}`
- `GET /api/notifications/account/{accountNumber}`

Chi tiết đầy đủ xem tại:
- [analysis-and-design-ddd.md](docs/analysis-and-design-ddd.md)
- [architecture.md](docs/architecture.md)
- [docs/api-specs/auth-service.yaml](docs/api-specs/auth-service.yaml)
- [docs/api-specs/account-service.yaml](docs/api-specs/account-service.yaml)
- [docs/api-specs/transaction-service.yaml](docs/api-specs/transaction-service.yaml)
- [docs/api-specs/fraud-detection-service.yaml](docs/api-specs/fraud-detection-service.yaml)
- [docs/api-specs/notification-service.yaml](docs/api-specs/notification-service.yaml)
- [docs/api-specs/audit-service.yaml](docs/api-specs/audit-service.yaml)

## 9. URL observability

Khi chạy profile `full`:
- Frontend: `http://localhost/`
- Kong Admin: `http://localhost:8001`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Jaeger: `http://localhost:16686`
- Kibana: `http://localhost:5601`

Grafana mặc định:
- `admin / admin`

### Nên kiểm tra gì

Prometheus:
- `http_server_requests_seconds`
- `banking_auth_register_success_total`
- `banking_auth_login_success_total`
- `banking_transfer_requests_total`
- `banking_transfer_duration`
- `banking_fraud_decisions_total`
- `banking_notification_events_total`
- `banking_audit_events_total`

Jaeger:
- xem trace của `transaction-service`
- đối chiếu outbound call sang `fraud-detection-service` và `account-service`
- xem trace tiếp tục ở notification-service và audit-service nếu có

Logs:
- kiểm tra `traceId`
- kiểm tra `spanId`
- kiểm tra `requestId`
- đối chiếu log của transfer với trace tương ứng

## 10. Kiểm thử hiện có

Dự án hiện có test cho các phần chính:
- auth controller
- account controller
- transfer saga
- transfer controller / idempotency / outbox
- notification consumer
- audit consumer
- fraud controller

## 11. Một số lưu ý kỹ thuật

- `account-service` không đi qua Kong để tránh cấu hình chồng chéo với Nginx.
- `Idempotency-Key` chỉ chống retry cùng request, không tự động biến hai lần bấm khác nhau thành một giao dịch.
- `outbox_events` đảm bảo event không bị mất khi Kafka lỗi tạm thời.
- `notification-service` hiện chỉ tạo thông báo khi event type là `TRANSFER_COMPLETED`.
- `audit-service` không có business API public, chủ yếu hoạt động như Kafka consumer + actuator endpoint.

## 12. Xử lý sự cố nhanh

Nếu hệ thống lỗi, kiểm tra theo thứ tự:

1. `docker ps`
2. `docker compose -f infra/docker-compose.yml --profile core logs kong`
3. log của từng service backend
4. kết nối PostgreSQL / Redis / Kafka
5. endpoint `/actuator/health` của từng service
6. Prometheus / Jaeger nếu đang chạy profile `full`

## 13. Lệnh tiện ích

Kiểm tra reactor Maven:

```powershell
mvn -q validate
```

Dừng service trên Linux:

```bash
./scripts/stop-services.sh
```
