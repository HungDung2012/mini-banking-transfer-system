# Mini Banking Transfer System

Modules live under `services/`, infrastructure lives under `infra/`, and the browser UI lives under `frontend/`.
Use `mvn -q validate` to verify the bootstrap reactor.

## Demo Flow

1. Start the local stack with `docker compose -f infra/docker-compose.yml up -d`.
2. Seed the demo users and accounts with `powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1`.
3. Run the smoke verification with `powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1`.
4. Open the frontend in a browser and use the seeded `alice` account to log in and inspect the transfer result.

The demo scripts use the Kong gateway at `http://localhost:8000` by default. The seed script talks to the auth and account services directly, and both scripts allow overriding base URLs with environment variables if the local ports are different.