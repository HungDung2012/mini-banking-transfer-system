# Audit Service

## Tổng quan

Service này phụ trách lưu vết audit cho giao dịch chuyển tiền.
- Sở hữu dữ liệu audit trong `audit_db`.
- Tiêu thụ transfer event từ Kafka.
- Lưu bản ghi audit bất biến để phục vụ truy vết và đối soát.
- Không cung cấp business API public trong implementation hiện tại.

## Công nghệ sử dụng

| Thành phần | Lựa chọn |
|------------|----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot, Spring Data JPA, Spring Kafka |
| Cơ sở dữ liệu | PostgreSQL |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/health` | Health check đơn giản |
| GET | `/actuator/health` | Kiểm tra trạng thái service |
| GET | `/actuator/prometheus` | Xuất metrics cho Prometheus |

> Đặc tả API đầy đủ: [`../../docs/api-specs/audit-service.yaml`](../../docs/api-specs/audit-service.yaml)

## Chạy local

```bash
# Từ thư mục gốc của dự án
mvn -q -f services/audit-service/pom.xml spring-boot:run
```

## Cấu trúc thư mục

```text
audit-service/
pom.xml
readme.md
src/
```

## Biến môi trường

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/audit_db` |
| `SPRING_DATASOURCE_USERNAME` | Tài khoản database | `banking` |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu database | `banking` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Địa chỉ Kafka bootstrap server | `localhost:29092` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Địa chỉ OTLP exporter | `http://localhost:4318/v1/traces` |