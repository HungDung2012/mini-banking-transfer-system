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

### 4. Run the smoke test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1
```

By default the smoke test uses Kong at `http://localhost:8000`.

### 5. Open the frontend

Open `frontend/index.html` in a browser after the services are running, then log in with:

- username: `alice`
- password: `secret123`

## Demo Flow

1. Start the lighter dev stack with `docker compose -f infra/docker-compose.yml --profile core up -d`.
2. Start each Spring Boot service from `services/` in its own terminal.
3. Seed the demo users and accounts with `powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1`.
4. Run the smoke verification with `powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1`.
5. Open the frontend in a browser and use the seeded `alice` account to log in and inspect the transfer result.

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
