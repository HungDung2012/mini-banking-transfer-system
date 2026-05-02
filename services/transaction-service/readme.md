# Transaction Service

## Tổng quan

Service này phụ trách điều phối quy trình chuyển tiền.
- Sở hữu dữ liệu giao dịch và outbox trong `transaction_db`.
- Kiểm tra idempotency để tránh xử lý trùng.
- Gọi fraud-service, account-service và điều phối bù trừ khi cần.
- Phát transfer event để notification-service và audit-service xử lý bất đồng bộ.

## Công nghệ sử dụng

| Thành phần | Lựa chọn |
|------------|----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot, Spring Data JPA, Spring Data Redis, Spring Kafka |
| Cơ sở dữ liệu | PostgreSQL + Redis |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/transfers` | Tạo và xử lý yêu cầu chuyển tiền |
| GET | `/health` | Health check đơn giản |
| GET | `/actuator/health` | Kiểm tra trạng thái service |
| GET | `/actuator/prometheus` | Xuất metrics cho Prometheus |

> Đặc tả API đầy đủ: [`../../docs/api-specs/transaction-service.yaml`](../../docs/api-specs/transaction-service.yaml)

## Chạy local

```bash
# Từ thư mục gốc của dự án
mvn -q -f services/transaction-service/pom.xml spring-boot:run
```

## Cấu trúc thư mục

```text
transaction-service/
pom.xml
readme.md
src/
```

## Biến môi trường

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/transaction_db` |
| `SPRING_DATASOURCE_USERNAME` | Tài khoản database | `banking` |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu database | `banking` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Địa chỉ Kafka bootstrap server | `localhost:29092` |
| `SPRING_REDIS_HOST` | Địa chỉ Redis host | `localhost` |
| `SPRING_REDIS_PORT` | Cổng Redis | `6379` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Địa chỉ OTLP exporter | `http://localhost:4318/v1/traces` |