# Account Service

## Tổng quan

Service này phụ trách miền quản lý tài khoản.
- Sở hữu dữ liệu tài khoản và số dư trong `account_db`.
- Cung cấp API tạo tài khoản, truy vấn tài khoản, debit, credit và compensate.
- Phục vụ cả frontend và transaction-service.

## Công nghệ sử dụng

| Thành phần | Lựa chọn |
|------------|----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot, Spring Data JPA |
| Cơ sở dữ liệu | PostgreSQL |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/accounts` | Tạo tài khoản mới |
| GET | `/accounts/{accountNumber}` | Lấy thông tin tài khoản theo số tài khoản |
| GET | `/accounts/by-owner/{ownerName}` | Lấy tài khoản theo tên chủ sở hữu |
| POST | `/accounts/debit` | Trừ tiền tài khoản |
| POST | `/accounts/credit` | Cộng tiền tài khoản |
| POST | `/accounts/compensate` | Hoàn tiền bù trừ |
| GET | `/actuator/health` | Kiểm tra trạng thái service |
| GET | `/actuator/prometheus` | Xuất metrics cho Prometheus |

> Đặc tả API đầy đủ: [`../../docs/api-specs/account-service.yaml`](../../docs/api-specs/account-service.yaml)

## Chạy local

```bash
# Từ thư mục gốc của dự án
mvn -q -f services/account-service/pom.xml spring-boot:run
```

## Cấu trúc thư mục

```text
account-service/
pom.xml
readme.md
src/
```

## Biến môi trường

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/account_db` |
| `SPRING_DATASOURCE_USERNAME` | Tài khoản database | `banking` |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu database | `banking` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Địa chỉ OTLP exporter | `http://localhost:4318/v1/traces` |