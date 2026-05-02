# Auth Service

## Tổng quan

Service này phụ trách miền xác thực và truy cập người dùng.
- Quản lý đăng ký và đăng nhập.
- Sở hữu dữ liệu người dùng trong `auth_db`.
- Cấp JWT cho client sau khi xác thực thành công.
- Đồng bộ consumer tương ứng với người dùng vào Kong.

## Công nghệ sử dụng

| Thành phần | Lựa chọn |
|------------|----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot, Spring Security, Spring Data JPA |
| Cơ sở dữ liệu | PostgreSQL |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/auth/register` | Đăng ký người dùng mới |
| POST | `/auth/login` | Đăng nhập và nhận JWT |
| GET | `/actuator/health` | Kiểm tra trạng thái service |
| GET | `/actuator/prometheus` | Xuất metrics cho Prometheus |

> Đặc tả API đầy đủ: [`../../docs/api-specs/auth-service.yaml`](../../docs/api-specs/auth-service.yaml)

## Chạy local

```bash
# Từ thư mục gốc của dự án
mvn -q -f services/auth-service/pom.xml spring-boot:run
```

## Cấu trúc thư mục

```text
auth-service/
pom.xml
readme.md
src/
```

## Biến môi trường

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL của PostgreSQL | `jdbc:postgresql://localhost:5432/auth_db` |
| `SPRING_DATASOURCE_USERNAME` | Tài khoản database | `banking` |
| `SPRING_DATASOURCE_PASSWORD` | Mật khẩu database | `banking` |
| `KONG_ADMIN_URL` | Địa chỉ Kong Admin API | `http://localhost:8001` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Địa chỉ OTLP exporter | `http://localhost:4318/v1/traces` |