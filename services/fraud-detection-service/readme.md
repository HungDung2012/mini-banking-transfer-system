# Fraud Detection Service

## Tổng quan

Service này phụ trách kiểm tra gian lận cho giao dịch chuyển tiền.
- Kiểm tra các luật nghiệp vụ như tài khoản nguồn và đích không được trùng nhau.
- Kiểm tra ngưỡng số tiền giao dịch.
- Trả về quyết định `APPROVED` hoặc `REJECTED` cho transaction-service.

## Công nghệ sử dụng

| Thành phần | Lựa chọn |
|------------|----------|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot |
| Cơ sở dữ liệu | Không sử dụng |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/fraud/check` | Kiểm tra gian lận cho giao dịch |
| GET | `/health` | Health check đơn giản |
| GET | `/actuator/health` | Kiểm tra trạng thái service |
| GET | `/actuator/prometheus` | Xuất metrics cho Prometheus |

> Đặc tả API đầy đủ: [`../../docs/api-specs/fraud-detection-service.yaml`](../../docs/api-specs/fraud-detection-service.yaml)

## Chạy local

```bash
# Từ thư mục gốc của dự án
mvn -q -f services/fraud-detection-service/pom.xml spring-boot:run
```

## Cấu trúc thư mục

```text
fraud-detection-service/
pom.xml
readme.md
src/
```

## Biến môi trường

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Địa chỉ OTLP exporter | `http://localhost:4318/v1/traces` |