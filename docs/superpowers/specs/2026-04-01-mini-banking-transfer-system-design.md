# Mini Banking Transfer System Design

## Overview

This project is a course assignment for Service-Oriented Software Development. The frontend stays intentionally simple: a user logs in, enters a source account, destination account, and transfer amount, then submits the transfer and views the result. The backend is the main focus and is designed as a realistic microservice architecture with gateway, authentication, event-driven communication, resilience patterns, observability, and database-per-service boundaries.

## Goals

- Build a realistic microservice-based banking transfer system suitable for a major course assignment
- Keep the frontend simple so implementation effort focuses on backend architecture
- Demonstrate both synchronous and asynchronous service communication
- Include important enterprise patterns: saga orchestration, circuit breaker, idempotency key, outbox pattern, audit logging, and observability
- Run the full system locally with Docker Compose

## Scope

### In Scope

- Simple frontend for login and transfer submission
- API gateway for routing and auth enforcement
- JWT-based authentication service
- Transaction orchestration service for transfer processing
- Account service for account ownership and balance changes
- Fraud detection service with simple rule-based checks
- Notification service consuming transfer events
- Audit service storing immutable event logs
- Kafka-based event transport
- PostgreSQL database per service
- Redis for idempotency and distributed locking support
- Observability with OpenTelemetry, Jaeger, Prometheus, Grafana, and centralized logs

### Out of Scope

- Full banking domain such as account opening, cards, loans, or admin dashboards
- Complex ML-based fraud detection
- Full OAuth2 authorization server implementation
- Event-sourced rebuild of the entire business state
- Multi-page customer portal

## System Architecture

### High-Level Topology

```text
Frontend (React or HTML)
    -> API Gateway (recommended: Kong)
        -> Auth Service (JWT login/register)
        -> Transaction Service (Saga orchestrator)
            -> Account Service
            -> Fraud Detection Service
            -> Kafka Topic: transfer-events
                -> Notification Service
                -> Audit Service

Observability Layer:
    OpenTelemetry -> OpenTelemetry Collector -> Jaeger
    Prometheus -> Grafana
    Service/container logs -> Filebeat -> Elasticsearch -> Kibana
```

A single `transfer-events` topic is used. The event payload includes an `eventType` field such as `TRANSFER_COMPLETED`, `TRANSFER_FAILED`, `TRANSFER_COMPENSATED`, or `TRANSFER_REJECTED`, which consumers use to filter and route processing.
### Architectural Style

- Microservices with clear service ownership
- API Gateway as the single frontend entry point
- Synchronous REST for core business orchestration
- Asynchronous Kafka events for downstream consumers
- Database per service
- Event-driven audit and notification
- Local containerized deployment with Docker Compose

## Infrastructure

### Runtime Components

- `frontend`
- `kong`
- `auth-service`
- `transaction-service`
- `account-service`
- `fraud-detection-service`
- `notification-service`
- `audit-service`
- `kafka`
- `zookeeper`
- `redis`
- one PostgreSQL container hosting separate databases for each service
- `otel-collector`
- `jaeger`
- `prometheus`
- `grafana`
- `filebeat`
- `elasticsearch`
- `kibana`

### Infrastructure Decisions

- Kong is preferred over plain Nginx because it better matches API gateway responsibilities and presentation value
- Kafka is preferred over RabbitMQ because it fits the event backbone and audit-stream story well
- PostgreSQL is used with the database-per-service pattern
- One PostgreSQL container with separate databases per service is a local development simplification; in production each service would own its own isolated PostgreSQL instance
- Redis is used for idempotency keys and optional distributed locking
- Filebeat replaces Logstash to reduce local complexity while keeping centralized log shipping
- OpenTelemetry Collector is included so tracing and telemetry flow through a proper observability pipeline
- Kafka will run with Zookeeper for this assignment because it is easier to set up, explain, and troubleshoot in a local Docker Compose environment

## Service Responsibilities

### API Gateway

- Public entry point for the frontend
- Routes requests to backend services
- Applies auth middleware and request forwarding
- Hides internal service topology from the frontend

### Auth Service

- Handles user registration and login
- Issues JWT tokens
- Supports token validation for protected routes
- Keeps auth minimal and practical for coursework

### Transaction Service

- Core transfer business logic
- Main saga orchestrator
- Validates transfer requests
- Checks and stores idempotency keys
- Calls fraud detection before balance mutation
- Coordinates debit and credit operations with account service
- Executes compensation when a partial failure occurs
- Persists transfer state
- Writes outbox events for Kafka publication

### Account Service

- Owns account records and balances
- Exposes account lookup and balance mutation endpoints
- Is the only service allowed to mutate balances

### Fraud Detection Service

- Performs simple rule-based fraud checks
- Examples: large amount threshold, blocked accounts, same-account transfer rule, suspicious patterns
- Returns allow or reject decisions

### Notification Service

- Consumes Kafka transfer events
- Generates mock notifications for success or failure
- Must behave as an idempotent consumer because outbox-driven Kafka delivery is at-least-once
- Should deduplicate events before processing, preferably by `eventId` and, if needed, by `transferId`
- Can log to console and optionally persist notification history

### Audit Service

- Consumes Kafka transfer events
- Stores immutable append-only audit records
- Must behave as an idempotent consumer because outbox-driven Kafka delivery is at-least-once
- Should deduplicate events before processing, preferably by `eventId` and, if needed, by `transferId`
- Provides traceability for system activity
- Represents event logging and audit trail, not full business-state reconstruction

## Core Data Ownership

### Auth Service Database

- users
- roles or authorities if needed

### Account Service Database

- accounts
- balance-related state

### Transaction Service Database

- transfers
- idempotency records or references
- outbox table

### Notification Service Database

- notifications

### Audit Service Database

- audit_events

No service should read or write another service's database directly.

## API Surface

### Frontend to Gateway

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/transfers`
- `GET /api/transfers/{transferId}`

### Transaction Request Example

```json
{
  "sourceAccount": "100001",
  "destinationAccount": "200001",
  "amount": 500000
}
```

Headers:

```text
Authorization: Bearer <jwt>
Idempotency-Key: <uuid>
```

### Transaction Response Example

```json
{
  "transferId": "TRF-001",
  "status": "SUCCESS",
  "message": "Transfer completed successfully"
}
```

Transfer status values:

- `PENDING` — created and being processed
- `SUCCESS` — all steps completed
- `FAILED` — failed before any debit and no compensation is needed
- `COMPENSATED` — debit succeeded but credit failed, and refund compensation completed
- `REJECTED` — blocked by a fraud detection rule

### Internal Service APIs

Transaction service calls:
- fraud detection check endpoint
- account lookup if needed
- debit source account endpoint
- credit destination account endpoint
- compensation or refund endpoint when rollback is needed

Downstream services should not call each other arbitrarily. Transaction service remains the orchestrator.

## Transfer Flow

### Success Path

1. User logs in and receives JWT
2. Frontend submits transfer request to gateway with JWT and idempotency key
3. Gateway validates forwarding/auth policy and routes to transaction service
4. Transaction service validates request shape and idempotency
5. Transaction service calls fraud detection service
6. If fraud check passes, transaction service requests debit from account service
7. Transaction service requests credit from account service
8. Transaction service marks transfer as successful
9. Transaction service writes an outbox event in the same local transaction
   The response is returned to the frontend after step 8. Steps 10-12 are asynchronous and do not block the response.
10. Outbox publisher sends the event to Kafka
11. Notification service consumes the event and creates a notification
12. Audit service consumes the event and stores an immutable audit log

### Failure and Compensation Path

1. Request enters transaction service
2. Fraud detection or debit or credit step fails
3. If debit already succeeded but credit fails, transaction service triggers compensation to refund the source account
4. Transaction service marks transfer as failed with a reason
5. Failed transfer event is written to outbox and published to Kafka
6. Audit and notification services process the failure event
7. Frontend receives failed response

## Important Patterns

### Saga Pattern

- Orchestration-based saga
- Implemented in transaction service
- Handles multi-step transfer without distributed transactions
- Uses compensation to restore state after partial failure

### Circuit Breaker

- Implemented with Resilience4j
- Applied on transaction service calls to fraud detection service
- Applied on transaction service calls to account service for debit, credit, and compensation operations
- Protects the core flow when fraud detection or account service becomes slow or unavailable

### Idempotency Key

- Required for transfer submission
- Stored and checked via Redis
- Prevents duplicate processing caused by retries or repeated button clicks

### Outbox Pattern

- Transaction service stores transfer state and event in one local transaction
- A background publisher relays outbox entries to Kafka
- Prevents lost messages when the service crashes after DB commit
- Delivery semantics are at-least-once, so downstream consumers must tolerate duplicate events
- Notification service and audit service must be idempotent consumers and deduplicate by `eventId` or `transferId` before applying side effects

## Observability

### Tracing

- Services emit telemetry using OpenTelemetry
- Telemetry is sent to OpenTelemetry Collector
- Jaeger visualizes distributed traces
- Transfer operations should share a correlation or trace ID across services

### Metrics

- Services expose metrics for Prometheus scraping
- Grafana provides dashboards
- Suggested metrics:
  - request count
  - request latency
  - transfer success/failure count
  - fraud check failures
  - Kafka consumer lag where practical

### Logs

- Service and container logs are collected by Filebeat
- Logs are shipped to Elasticsearch
- Kibana is used to inspect logs during demo and troubleshooting

## Error Handling Rules

- Invalid amount: reject immediately
- Source account not found: fail transfer
- Destination account not found: fail transfer
- Source equals destination: reject immediately
- Insufficient balance: fail transfer
- Fraud rule violation: fail transfer
- Fraud service timeout or unavailability: circuit breaker/fallback policy should produce a controlled failure rather than hanging the request
- Credit failure after successful debit: execute compensation rollback

## Security

- All transfer endpoints require JWT
- Kong is the single JWT validation point for protected external requests
- After successful JWT validation, Kong forwards trusted identity metadata such as `X-User-Id` to internal services
- Internal services trust gateway-forwarded identity headers and do not re-validate JWTs in this coursework design
- Keep security implementation practical, not enterprise-identity-platform-level
## Testing Strategy

### Unit Tests

- validation rules
- saga decision logic
- fraud rule logic
- idempotency handling

### Integration Tests

- transaction service to account service REST interaction
- transaction service to fraud detection REST interaction
- persistence of outbox records

### Messaging Tests

- outbox publisher to Kafka
- notification consumer behavior
- audit consumer behavior

### End-to-End Tests or Demo Verification

- successful transfer
- insufficient balance failure
- destination account not found
- fraud rejection
- compensation rollback scenario
- duplicate submission with same idempotency key

## Demo Plan

1. Start the entire stack with Docker Compose
2. Show Grafana, Jaeger, or Kibana ready for observation
3. Log in from the frontend and obtain access
4. Submit a valid transfer
5. Show successful UI response
6. Show updated balances or transfer state
7. Show Kafka-driven notification and audit records
8. Show trace/metrics/log evidence
9. Submit a failing transfer
10. Show controlled failure and audit trace
11. If possible, demonstrate compensation and idempotency behavior

## Suggested Module Layout

```text
frontend/
gateway/
auth-service/
transaction-service/
account-service/
fraud-detection-service/
notification-service/
audit-service/
infra/
  docker-compose.yml
  kong/
  kafka/
  postgres/
  redis/
  observability/
```

## Delivery Summary

The final assignment should present a simple user-facing transfer flow backed by a realistic microservice architecture. The backend demonstrates gateway routing, JWT authentication, saga orchestration, fraud validation, balance management, Kafka-based events, immutable audit logging, outbox reliability, Redis-backed idempotency, and a full observability stack. This keeps the UI small while making the architecture rich enough for a major service-oriented software development project.


