# Mini Banking Transfer System

Modules live under `services/`, infrastructure lives under `infra/`, and the browser UI lives under `frontend/`.
Use `mvn -q validate` to verify the bootstrap reactor.

## How To Run

`infra/docker-compose.yml` only starts shared infrastructure such as Kong, Kafka, Redis, PostgreSQL, Jaeger, Grafana, and Elasticsearch.
The Spring Boot services under `services/` are not part of the Compose file, so they must be started separately in their own terminals.

### 1. Start infrastructure

For day-to-day development on a weaker machine, start only the core stack:

```powershell
docker compose -f infra/docker-compose.yml --profile core up -d
```

This starts only `kong`, `zookeeper`, `kafka`, `kafka-init`, `redis`, and `postgres`.

When you need the full demo stack with observability, use:

```powershell
docker compose -f infra/docker-compose.yml --profile full up -d
```

### 2. Start backend services in separate terminals

You can either start all service windows automatically:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-services.ps1
```

Or run each command below in its own terminal window:

```powershell
mvn -q -pl services/auth-service spring-boot:run
mvn -q -pl services/account-service spring-boot:run
mvn -q -pl services/fraud-detection-service spring-boot:run
mvn -q -pl services/transaction-service spring-boot:run
mvn -q -pl services/notification-service spring-boot:run
mvn -q -pl services/audit-service spring-boot:run
```

### 3. Seed demo data

```powershell
powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1
```

On Linux or Google Cloud VMs, use:

```bash
./scripts/seed-demo-data.sh
```

By default the seed script calls:
- auth via Kong at `http://localhost:8000/api/auth/register`
- account-service directly at `http://localhost:8082/accounts`

If your local ports are different, override them with `AUTH_BASE_URL` and `ACCOUNT_BASE_URL`.
The main services now default to PostgreSQL on `localhost:5432`, so auth/account/transaction data survives service restarts and VM redeploys as long as the Postgres volume is preserved.

### 4. Run the smoke test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1
```

By default the smoke test uses Kong at `http://localhost:8000`.

### 5. Open the frontend

Open `frontend/index.html` in a browser after the services are running, then log in with:

- username: `alice`
- password: `secret123`

When deployed on the VM, open the VM public IP on port `80` to access the frontend.
API endpoints such as `/api/auth/login` are backend routes and are not meant to be opened directly in a browser tab.

## Demo Flow

1. Start the lighter dev stack with `docker compose -f infra/docker-compose.yml --profile core up -d`.
2. Start each Spring Boot service from `services/` in its own terminal.
3. Seed the demo users and accounts with `powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1`.
4. Run the smoke verification with `powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1`.
5. Open the frontend in a browser and use the seeded `alice` account to log in and inspect the transfer result.
6. Log in as `bob` after a successful transfer from `alice` to confirm the destination account sees the incoming transfer notification.

The demo scripts use the Kong gateway at `http://localhost:8000` by default. The seed script talks to the auth and account services directly, and both scripts allow overriding base URLs with environment variables if the local ports are different.

## Deployment Note

For deployment on a Google Cloud VM, the same Compose file can be used:

- use `--profile core` for regular testing and CI smoke checks
- use `--profile full` only when you need the complete observability demo

This keeps the repository architecture intact while reducing RAM pressure during development.

For Linux-based VM hosts, use:

```bash
./scripts/start-services.sh
./scripts/stop-services.sh
./scripts/seed-demo-data.sh
```

A starter GitHub Actions workflow is available at `.github/workflows/deploy-google-vm.yml`.

## VM Deploy

This project is deployed to a Google Cloud `e2-standard-4` VM.

- VM name: `mini-banking-vm`
- Zone: `us-central1-a`
- CI/CD target profile: `core`
- Demo-only profile: `full`

Required software on the VM:

- Docker Engine
- `docker-compose`
- Git
- Maven
- Java 21

Manual deploy flow on the VM:

```bash
git clone https://github.com/HungDung2012/mini-banking-transfer-system.git
cd mini-banking-transfer-system
chmod +x scripts/*.sh
sudo docker-compose -f infra/docker-compose.yml --profile core up -d
./scripts/start-services.sh
./scripts/seed-demo-data.sh
```

GitHub Actions repository configuration:

- Secrets:
  - `GCP_WORKLOAD_IDENTITY_PROVIDER`
  - `GCP_SERVICE_ACCOUNT`
- Variables:
  - `GCP_PROJECT_ID`
  - `GCP_COMPUTE_ZONE`
  - `GCP_VM_NAME`

Workflow behavior:

1. Authenticate to Google Cloud with Workload Identity Federation.
2. Package the current commit with `git archive`.
3. Copy the bundle to the VM.
4. Reset the `core` Compose stack on the VM.
5. Restart backend services on the VM.

## Demo Checklist

Before the demo:

1. Confirm the latest code is pushed to `main`.
2. Confirm the deploy workflow has succeeded.
3. Confirm the VM is running.
4. Confirm the `core` stack is up.
5. Confirm backend services are running.

Demo credentials:

- Username: `alice`
- Password: `secret123`
- Username: `bob`
- Password: `secret123`

Notification behavior:

- Incoming transfer notifications are shown for the destination account holder.
- Example: transfer from `alice` account `100001` to `bob` account `200001`, then log in as `bob` to see the received-money notification.
- Registering an existing username returns a conflict and should be handled as "username already exists".

Demo commands on Windows:

```powershell
docker compose -f infra/docker-compose.yml --profile core up -d
powershell -ExecutionPolicy Bypass -File scripts/start-services.ps1
powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1
powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1
```

Demo commands on Linux / VM:

```bash
sudo docker-compose -f infra/docker-compose.yml --profile core up -d
./scripts/start-services.sh
./scripts/seed-demo-data.sh
curl http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

If something fails, check:

1. `docker logs infra_kong_1`
2. `logs/auth-service.log`
3. `logs/account-service.log`
4. `logs/transaction-service.log`

1. Đăng ký / đăng nhập

Frontend gọi /api/auth/register hoặc /api/auth/login.
Route này đi qua Kong theo infra/kong/kong.yml.
Trong AuthService.java:
register() lưu user, rồi gọi kongProvisioningService.ensureConsumer(...), sau đó mới sinh JWT.
login() kiểm tra mật khẩu đúng, rồi cũng gọi ensureConsumer(...), sau đó sinh JWT.
Việc provision Kong nằm ở KongProvisioningService.java:
tạo consumer trong Kong
tạo JWT secret cho consumer đó
Điểm rất đáng nói với thầy:
nếu Kong provisioning lỗi trong lúc đăng ký thì transaction auth bị rollback và trả 503
điều này được test ở AuthControllerIT.java
2. Liên kết user với account

Sau khi login, frontend gọi /api/accounts/by-owner/{ownerName} để tìm account number theo username.
Code nằm ở frontend/app.js, hàm resolveAccountNumberForUser(...).
Backend hỗ trợ ở AccountController.java với endpoint:
GET /accounts/by-owner/{ownerName}
Logic tìm theo owner name ở AccountCommandService.java, method getByOwnerName(...).
Nếu chưa có account, frontend có thể tạo account mới bằng POST /api/accounts.
3. Chuyển tiền

Frontend gửi transfer tới /api/transfers kèm Authorization: Bearer ... và Idempotency-Key.
Trong frontend/app.js, phần này nằm ở handleTransfer(...).
Transaction Service vẫn là trung tâm điều phối saga ở TransferApplicationService.java:
tạo giao dịch PENDING
gọi fraud
debit nguồn
credit đích
nếu credit lỗi thì compensate
cập nhật trạng thái cuối
ghi outbox event
TransferController ở TransferController.java còn cho thấy nó lấy danh tính user từ header:
X-User-Id
hoặc X-Consumer-Username
Đây là điểm bạn có thể nói: transaction-service đang nhận identity ở tầng gateway/header thay vì tự làm auth đầy đủ bên trong.
4. Thông báo

Frontend giờ không poll /api/notifications chung nữa mà poll theo tài khoản:
/api/notifications/account/{accountNumber}
Điều này nằm trong frontend/app.js, hàm fetchNotifications().
Backend query ở NotificationController.java.
Logic đọc thông báo theo tài khoản nằm ở NotificationQueryService.java và NotificationRepository.java.
5. Notification được tạo như thế nào

Ở TransferNotificationConsumer.java:
nếu event đã xử lý rồi thì bỏ qua
nếu eventType không phải TRANSFER_COMPLETED thì không tạo notification, chỉ lưu processed marker
nếu là TRANSFER_COMPLETED thì tạo:
NotificationEntity.incoming(event)
NotificationEntity.outgoing(event)
Nội dung notification nằm ở NotificationEntity.java:
incoming: “Bạn vừa nhận ...”
outgoing: “Bạn vừa chuyển ...”
Test xác nhận hành vi này ở TransferNotificationConsumerTest.java