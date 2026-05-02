# Notification Service

## Tổng quan

Service này phụ trách thông báo sau giao dịch.
- Sở hữu dữ liệu thông báo trong `notification_db`.
- Tiêu thụ transfer event từ Kafka.
- Tạo thông báo cho bên gửi và bên nhận.
- Cung cấp API truy vấn thông báo theo tài khoản.

## Công nghệ sử dụng

| Thành phần | Lựa chọn |
|------------|----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot, Spring Data JPA, Spring Kafka |
| Cơ sở dữ liệu | PostgreSQL |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/notifications/account/{accountNumber}` | Lấy danh sách thông báo theo tài khoản |
| GET | `/health` | Health check đơn giản |
| GET | `/actuator/health` | Kiểm tra trạng thái service |
| GET | `/actuator/prometheus` | Xuất metrics cho Prometheus |

> Đặc tả API đầy đủ: [`../../docs/api-specs/notification-service.yaml`](../../docs/api-specs/notification-service.yaml)

## Chạy local

```bash
# Từ thư mục gốc của dự án
mvn -q -f services/notification-service/pom.xml spring-boot:run
```

## Cấu trúc thư mục

```text
notification-service/
pom.xml
readme.md
src/
```

## Biến môi trường

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/notification_db` |
| `SPRING_DATASOURCE_USERNAME` | Tài khoản database | `banking` |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu database | `banking` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Địa chỉ Kafka bootstrap server | `localhost:29092` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Địa chỉ OTLP exporter | `http://localhost:4318/v1/traces` |