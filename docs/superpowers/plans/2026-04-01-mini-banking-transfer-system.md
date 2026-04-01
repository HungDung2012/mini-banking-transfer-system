# Mini Banking Transfer System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local, dockerized banking transfer system with Kong, Spring Boot microservices, Kafka, Redis, PostgreSQL, and observability, while keeping the frontend to a single login-and-transfer page.

**Architecture:** A Maven reactor hosts six Spring Boot services plus a static frontend and infrastructure configs. Kong validates JWTs and forwards `X-User-Id`; `transaction-service` orchestrates the transfer saga, writes outbox records, and publishes `transfer-events` to Kafka for idempotent notification and audit consumers.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Spring Cloud 2023.0.x, Spring Data JPA, Spring Security, Spring Kafka, Resilience4j, PostgreSQL 16, Redis 7, Kafka + Zookeeper, Kong, Docker Compose, OpenTelemetry, Jaeger, Prometheus, Grafana, Filebeat, Elasticsearch, Kibana, HTML/CSS/JavaScript.

---

## File Structure

- `pom.xml` — parent Maven reactor and dependency management
- `.gitignore` — repo ignores for Java, Docker, IDE, logs, and local secrets
- `README.md` — project bootstrap, run, and demo instructions
- `frontend/index.html` — single-page login + transfer UI
- `frontend/app.js` — frontend auth and transfer requests
- `frontend/styles.css` — simple page styling
- `infra/docker-compose.yml` — local stack orchestration
- `infra/kong/kong.yml` — declarative Kong routes, JWT plugin, and header forwarding
- `infra/postgres/init/00-create-databases.sql` — local dev database creation
- `infra/kafka/create-topics.sh` — creates `transfer-events`
- `infra/otel/otel-collector-config.yaml` — telemetry pipeline
- `infra/prometheus/prometheus.yml` — metric scraping
- `infra/filebeat/filebeat.yml` — log shipping to Elasticsearch
- `infra/grafana/provisioning/` — dashboard and datasource bootstrapping
- `services/auth-service/` — registration, login, JWT issuing
- `services/account-service/` — account CRUD, debit, credit, compensate
- `services/fraud-detection-service/` — rule-based fraud checks
- `services/transaction-service/` — transfer API, statuses, saga orchestration, idempotency, outbox, Kafka publisher
- `services/notification-service/` — idempotent Kafka consumer for user-facing notifications
- `services/audit-service/` — idempotent Kafka consumer for append-only audit events
- `scripts/smoke-auth-transfer.ps1` — end-to-end curl-based smoke test
- `scripts/seed-demo-data.ps1` — creates demo users and sample accounts

### Task 1: Bootstrap the Repository and Maven Reactor

**Files:**
- Create: `.gitignore`
- Create: `pom.xml`
- Create: `README.md`
- Create: `frontend/.gitkeep`
- Create: `infra/.gitkeep`
- Create: `services/.gitkeep`

- [ ] **Step 1: Initialize git and folders**

```powershell
New-Item -ItemType Directory -Force -Path frontend, infra, services, scripts, docs\superpowers\plans | Out-Null
if (-not (Test-Path .git)) { git init }
```

Expected: `.git`, `frontend`, `infra`, `services`, and `scripts` exist.

- [ ] **Step 2: Write the parent reactor and ignore rules**

```xml
<!-- file: pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.btlbanking</groupId>
  <artifactId>mini-banking-transfer-system</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>pom</packaging>
  <modules>
    <module>services/auth-service</module>
    <module>services/account-service</module>
    <module>services/fraud-detection-service</module>
    <module>services/transaction-service</module>
    <module>services/notification-service</module>
    <module>services/audit-service</module>
  </modules>
  <properties>
    <java.version>21</java.version>
    <spring.boot.version>3.3.5</spring.boot.version>
    <spring.cloud.version>2023.0.3</spring.cloud.version>
  </properties>
</project>
```

```gitignore
# file: .gitignore
.target/
**/target/
.idea/
.vscode/
.env
logs/
*.log
.superpowers/
frontend/.cache/
```

- [ ] **Step 3: Validate the root build**

Run: `mvn -q validate`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Write the top-level README**

```markdown
# Mini Banking Transfer System

Modules live under `services/`, infrastructure lives under `infra/`, and the browser UI lives under `frontend/`.
Use `docker compose -f infra/docker-compose.yml up -d` to start dependencies once service images exist.
```

- [ ] **Step 5: Commit**

```bash
git add .gitignore pom.xml README.md frontend/.gitkeep infra/.gitkeep services/.gitkeep
git commit -m "chore: bootstrap reactor workspace"
```

### Task 2: Scaffold `auth-service` with Register/Login and JWT Issuing

**Files:**
- Create: `services/auth-service/pom.xml`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/AuthServiceApplication.java`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/web/AuthController.java`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/service/AuthService.java`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/service/JwtService.java`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/user/UserEntity.java`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/user/UserRepository.java`
- Create: `services/auth-service/src/main/java/com/btlbanking/auth/config/SecurityBeans.java`
- Test: `services/auth-service/src/test/java/com/btlbanking/auth/AuthControllerIT.java`

- [ ] **Step 1: Write the failing auth integration test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {
  @Autowired MockMvc mockMvc;

  @Test
  void register_then_login_returns_jwt() throws Exception {
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"username":"alice","password":"secret123"}
        """))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"username":"alice","password":"secret123"}
        """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").isNotEmpty());
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl services/auth-service -Dtest=AuthControllerIT test`
Expected: FAIL because `auth-service` sources and Spring context do not exist yet.

- [ ] **Step 3: Write the minimal auth implementation**

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  AuthResponse register(@RequestBody AuthRequest request) { return authService.register(request); }

  @PostMapping("/login")
  AuthResponse login(@RequestBody AuthRequest request) { return authService.login(request); }
}
```

```java
@Service
@RequiredArgsConstructor
class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  AuthResponse register(AuthRequest request) {
    var user = new UserEntity(null, request.username(), passwordEncoder.encode(request.password()));
    userRepository.save(user);
    return new AuthResponse(jwtService.generateToken(user.getUsername(), user.getId()));
  }

  AuthResponse login(AuthRequest request) {
    var user = userRepository.findByUsername(request.username()).orElseThrow();
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw new BadCredentialsException("Invalid credentials");
    return new AuthResponse(jwtService.generateToken(user.getUsername(), user.getId()));
  }
}
```

- [ ] **Step 4: Re-run the auth tests**

Run: `mvn -q -pl services/auth-service -Dtest=AuthControllerIT test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/auth-service
git commit -m "feat: add auth service with jwt login"
```

### Task 3: Build `account-service` for Account CRUD, Debit, Credit, and Compensation

**Files:**
- Create: `services/account-service/pom.xml`
- Create: `services/account-service/src/main/java/com/btlbanking/account/AccountServiceApplication.java`
- Create: `services/account-service/src/main/java/com/btlbanking/account/domain/AccountEntity.java`
- Create: `services/account-service/src/main/java/com/btlbanking/account/domain/AccountRepository.java`
- Create: `services/account-service/src/main/java/com/btlbanking/account/service/AccountCommandService.java`
- Create: `services/account-service/src/main/java/com/btlbanking/account/web/AccountController.java`
- Test: `services/account-service/src/test/java/com/btlbanking/account/AccountControllerIT.java`

- [ ] **Step 1: Write the failing account workflow test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIT {
  @Autowired MockMvc mockMvc;

  @Test
  void debit_then_credit_then_compensate_updates_balance() throws Exception {
    mockMvc.perform(post("/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"accountNumber":"100001","ownerName":"Alice","balance":1000}"""))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/accounts/debit")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"accountNumber":"100001","amount":200}"""))
      .andExpect(status().isOk());

    mockMvc.perform(post("/accounts/compensate")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"accountNumber":"100001","amount":200}"""))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.balance").value(1000));
  }
}
```

- [ ] **Step 2: Run the account test to verify it fails**

Run: `mvn -q -pl services/account-service -Dtest=AccountControllerIT test`
Expected: FAIL because the service is not implemented yet.

- [ ] **Step 3: Implement the account model and endpoints**

```java
@Entity
@Table(name = "accounts")
class AccountEntity {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(unique = true, nullable = false)
  private String accountNumber;
  private String ownerName;
  private BigDecimal balance;
  @Enumerated(EnumType.STRING)
  private AccountStatus status;
}
```

```java
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
class AccountController {
  private final AccountCommandService service;

  @PostMapping AccountResponse create(@RequestBody CreateAccountRequest request) { return service.create(request); }
  @GetMapping("/{accountNumber}") AccountResponse get(@PathVariable String accountNumber) { return service.get(accountNumber); }
  @PostMapping("/debit") AccountResponse debit(@RequestBody BalanceCommand request) { return service.debit(request); }
  @PostMapping("/credit") AccountResponse credit(@RequestBody BalanceCommand request) { return service.credit(request); }
  @PostMapping("/compensate") AccountResponse compensate(@RequestBody BalanceCommand request) { return service.credit(request); }
}
```

- [ ] **Step 4: Re-run the account tests**

Run: `mvn -q -pl services/account-service -Dtest=AccountControllerIT test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/account-service
git commit -m "feat: add account service balance commands"
```
### Task 4: Build `fraud-detection-service` with Rule-Based Screening

**Files:**
- Create: `services/fraud-detection-service/pom.xml`
- Create: `services/fraud-detection-service/src/main/java/com/btlbanking/fraud/FraudDetectionServiceApplication.java`
- Create: `services/fraud-detection-service/src/main/java/com/btlbanking/fraud/service/FraudRuleService.java`
- Create: `services/fraud-detection-service/src/main/java/com/btlbanking/fraud/web/FraudController.java`
- Test: `services/fraud-detection-service/src/test/java/com/btlbanking/fraud/FraudControllerIT.java`

- [ ] **Step 1: Write the failing fraud test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class FraudControllerIT {
  @Autowired MockMvc mockMvc;

  @Test
  void large_transfer_is_rejected() throws Exception {
    mockMvc.perform(post("/fraud/check")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"sourceAccount":"100001","destinationAccount":"200001","amount":150000000}
        """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.decision").value("REJECTED"));
  }
}
```

- [ ] **Step 2: Run the fraud test to verify it fails**

Run: `mvn -q -pl services/fraud-detection-service -Dtest=FraudControllerIT test`
Expected: FAIL because the fraud service is not implemented yet.

- [ ] **Step 3: Implement simple fraud rules**

```java
@Service
class FraudRuleService {
  FraudCheckResponse evaluate(FraudCheckRequest request) {
    if (request.sourceAccount().equals(request.destinationAccount())) {
      return new FraudCheckResponse("REJECTED", "SOURCE_EQUALS_DESTINATION");
    }
    if (request.amount().compareTo(new BigDecimal("100000000")) > 0) {
      return new FraudCheckResponse("REJECTED", "AMOUNT_LIMIT_EXCEEDED");
    }
    return new FraudCheckResponse("APPROVED", "OK");
  }
}
```

- [ ] **Step 4: Re-run the fraud tests**

Run: `mvn -q -pl services/fraud-detection-service -Dtest=FraudControllerIT test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/fraud-detection-service
git commit -m "feat: add fraud detection rules"
```

### Task 5: Scaffold `transaction-service` with Transfer Statuses and Query Endpoint

**Files:**
- Create: `services/transaction-service/pom.xml`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/TransactionServiceApplication.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/domain/TransferEntity.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/domain/TransferStatus.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/domain/TransferRepository.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/web/TransferController.java`
- Test: `services/transaction-service/src/test/java/com/btlbanking/transaction/TransferControllerIT.java`

- [ ] **Step 1: Write the failing transfer API test**

```java
@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerIT {
  @Autowired MockMvc mockMvc;

  @Test
  void create_transfer_persists_and_get_by_id_returns_status() throws Exception {
    var result = mockMvc.perform(post("/transfers")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Key", "key-1")
        .header("X-User-Id", "user-1")
        .content("""{"sourceAccount":"100001","destinationAccount":"200001","amount":500}"""))
      .andExpect(status().isOk())
      .andReturn();

    var transferId = JsonPath.read(result.getResponse().getContentAsString(), "$.transferId");
    mockMvc.perform(get("/transfers/{id}", transferId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").exists());
  }
}
```

- [ ] **Step 2: Run the transfer test to verify it fails**

Run: `mvn -q -pl services/transaction-service -Dtest=TransferControllerIT test`
Expected: FAIL because the service and controller do not exist yet.

- [ ] **Step 3: Implement the transfer entity, statuses, and query API**

```java
public enum TransferStatus {
  PENDING,
  SUCCESS,
  FAILED,
  COMPENSATED,
  REJECTED
}
```

```java
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
class TransferController {
  private final TransferApplicationService service;

  @PostMapping
  TransferResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                          @RequestHeader("X-User-Id") String userId,
                          @RequestBody CreateTransferRequest request) {
    return service.create(userId, idempotencyKey, request);
  }

  @GetMapping("/{transferId}")
  TransferResponse get(@PathVariable UUID transferId) { return service.get(transferId); }
}
```

- [ ] **Step 4: Re-run the transfer tests**

Run: `mvn -q -pl services/transaction-service -Dtest=TransferControllerIT test`
Expected: PASS with the initial `PENDING` persistence flow.

- [ ] **Step 5: Commit**

```bash
git add services/transaction-service
git commit -m "feat: scaffold transaction service api"
```

### Task 6: Implement Saga Orchestration and Expand Circuit Breakers to Fraud and Account Calls

**Files:**
- Modify: `services/transaction-service/src/main/java/com/btlbanking/transaction/web/TransferController.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/service/TransferApplicationService.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/client/AccountClient.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/client/FraudClient.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/config/ResilienceConfig.java`
- Test: `services/transaction-service/src/test/java/com/btlbanking/transaction/TransferSagaServiceTest.java`

- [ ] **Step 1: Write failing saga tests for success, rejection, and compensation**

```java
@ExtendWith(MockitoExtension.class)
class TransferSagaServiceTest {
  @Mock AccountClient accountClient;
  @Mock FraudClient fraudClient;
  @Mock TransferRepository transferRepository;
  @InjectMocks TransferApplicationService service;

  @Test
  void rejected_fraud_sets_status_rejected() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("REJECTED", "AMOUNT_LIMIT_EXCEEDED"));
    var response = service.create("user-1", "idem-1", new CreateTransferRequest("100001", "200001", new BigDecimal("500")));
    assertThat(response.status()).isEqualTo("REJECTED");
    verifyNoInteractions(accountClient);
  }

  @Test
  void account_credit_failure_triggers_compensation_and_sets_compensated() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    doNothing().when(accountClient).debit("100001", new BigDecimal("500"));
    doThrow(new RuntimeException("credit down")).when(accountClient).credit("200001", new BigDecimal("500"));
    var response = service.create("user-1", "idem-2", new CreateTransferRequest("100001", "200001", new BigDecimal("500")));
    assertThat(response.status()).isEqualTo("COMPENSATED");
    verify(accountClient).compensate("100001", new BigDecimal("500"));
  }

  @Test
  void success_flow_sets_status_success() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    var response = service.create("user-1", "idem-3", new CreateTransferRequest("100001", "200001", new BigDecimal("500")));
    assertThat(response.status()).isEqualTo("SUCCESS");
    verify(accountClient).debit("100001", new BigDecimal("500"));
    verify(accountClient).credit("200001", new BigDecimal("500"));
  }
}
```

- [ ] **Step 2: Run the saga tests to verify they fail**

Run: `mvn -q -pl services/transaction-service -Dtest=TransferSagaServiceTest test`
Expected: FAIL because orchestration logic and clients are incomplete.

- [ ] **Step 3: Implement orchestration with circuit breakers**

```java
@Service
@RequiredArgsConstructor
class TransferApplicationService {
  @CircuitBreaker(name = "fraudService", fallbackMethod = "fraudUnavailable")
  public TransferResponse create(String userId, String idempotencyKey, CreateTransferRequest request) {
    var fraud = fraudClient.check(request);
    if ("REJECTED".equals(fraud.decision())) return updateStatus(request, TransferStatus.REJECTED, fraud.reason());
    return executeAccountSaga(userId, idempotencyKey, request);
  }

  @CircuitBreaker(name = "accountService", fallbackMethod = "accountUnavailable")
  TransferResponse executeAccountSaga(String userId, String idempotencyKey, CreateTransferRequest request) {
    accountClient.debit(request.sourceAccount(), request.amount());
    try {
      accountClient.credit(request.destinationAccount(), request.amount());
      return updateStatus(request, TransferStatus.SUCCESS, "Transfer completed successfully");
    } catch (Exception ex) {
      accountClient.compensate(request.sourceAccount(), request.amount());
      return updateStatus(request, TransferStatus.COMPENSATED, "Credit failed; refund applied");
    }
  }
}
```

- [ ] **Step 4: Re-run the transaction saga tests**

Run: `mvn -q -pl services/transaction-service -Dtest=TransferSagaServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/transaction-service
git commit -m "feat: orchestrate transfer saga with resilience"
```

### Task 7: Add Redis Idempotency, Outbox Persistence, and Kafka Publishing

**Files:**
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/idempotency/IdempotencyRecord.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/idempotency/IdempotencyStore.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/outbox/OutboxEventEntity.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/outbox/OutboxEventRepository.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/outbox/OutboxPublisher.java`
- Create: `services/transaction-service/src/main/java/com/btlbanking/transaction/events/TransferEvent.java`
- Test: `services/transaction-service/src/test/java/com/btlbanking/transaction/IdempotencyAndOutboxIT.java`

- [ ] **Step 1: Write the failing idempotency and outbox test**

```java
@SpringBootTest
class IdempotencyAndOutboxIT {
  @Autowired TransferApplicationService service;
  @Autowired OutboxEventRepository outboxRepository;

  @Test
  void duplicate_idempotency_key_returns_same_transfer_and_writes_one_outbox_record() {
    var first = service.create("user-1", "idem-1", new CreateTransferRequest("100001", "200001", new BigDecimal("500")));
    var second = service.create("user-1", "idem-1", new CreateTransferRequest("100001", "200001", new BigDecimal("500")));
    assertThat(second.transferId()).isEqualTo(first.transferId());
    assertThat(outboxRepository.findAll()).hasSize(1);
  }
}
```

- [ ] **Step 2: Run the idempotency/outbox test to verify it fails**

Run: `mvn -q -pl services/transaction-service -Dtest=IdempotencyAndOutboxIT test`
Expected: FAIL because Redis-backed idempotency and outbox storage are not implemented yet.

- [ ] **Step 3: Implement Redis dedupe and outbox event types**

```java
public enum TransferEventType {
  TRANSFER_COMPLETED,
  TRANSFER_FAILED,
  TRANSFER_COMPENSATED,
  TRANSFER_REJECTED
}
```

```java
@Component
@RequiredArgsConstructor
class IdempotencyStore {
  private final StringRedisTemplate redisTemplate;

  Optional<UUID> findTransferId(String key) { return Optional.ofNullable(redisTemplate.opsForValue().get(key)).map(UUID::fromString); }
  void save(String key, UUID transferId) { redisTemplate.opsForValue().set(key, transferId.toString(), Duration.ofHours(24)); }
}
```

```java
@Scheduled(fixedDelay = 1000)
void publishPendingEvents() {
  outboxRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc().forEach(event -> {
    kafkaTemplate.send("transfer-events", event.getAggregateId().toString(), event.getPayload());
    event.markPublished();
    outboxRepository.save(event);
  });
}
```

- [ ] **Step 4: Re-run the idempotency/outbox test**

Run: `mvn -q -pl services/transaction-service -Dtest=IdempotencyAndOutboxIT test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/transaction-service
git commit -m "feat: add idempotency and outbox publishing"
```
### Task 8: Build `notification-service` as an Idempotent Kafka Consumer

**Files:**
- Create: `services/notification-service/pom.xml`
- Create: `services/notification-service/src/main/java/com/btlbanking/notification/NotificationServiceApplication.java`
- Create: `services/notification-service/src/main/java/com/btlbanking/notification/domain/NotificationEntity.java`
- Create: `services/notification-service/src/main/java/com/btlbanking/notification/domain/ProcessedEventEntity.java`
- Create: `services/notification-service/src/main/java/com/btlbanking/notification/kafka/TransferNotificationConsumer.java`
- Test: `services/notification-service/src/test/java/com/btlbanking/notification/TransferNotificationConsumerTest.java`

- [ ] **Step 1: Write the failing duplicate-consumer test**

```java
@SpringBootTest
class TransferNotificationConsumerTest {
  @Autowired TransferNotificationConsumer consumer;
  @Autowired NotificationRepository notificationRepository;

  @Test
  void duplicate_event_is_processed_once() {
    var event = new TransferEvent("event-1", "TRANSFER_COMPLETED", UUID.randomUUID(), "SUCCESS");
    consumer.consume(event);
    consumer.consume(event);
    assertThat(notificationRepository.count()).isEqualTo(1);
  }
}
```

- [ ] **Step 2: Run the notification consumer test to verify it fails**

Run: `mvn -q -pl services/notification-service -Dtest=TransferNotificationConsumerTest test`
Expected: FAIL because no dedupe or consumer exists yet.

- [ ] **Step 3: Implement the idempotent Kafka listener**

```java
@Component
@RequiredArgsConstructor
class TransferNotificationConsumer {
  private final ProcessedEventRepository processedEventRepository;
  private final NotificationRepository notificationRepository;

  @KafkaListener(topics = "transfer-events", groupId = "notification-service")
  @Transactional
  public void consume(TransferEvent event) {
    if (processedEventRepository.existsByEventId(event.eventId())) return;
    notificationRepository.save(NotificationEntity.from(event));
    processedEventRepository.save(new ProcessedEventEntity(event.eventId(), event.transferId()));
  }
}
```

- [ ] **Step 4: Re-run the notification consumer test**

Run: `mvn -q -pl services/notification-service -Dtest=TransferNotificationConsumerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/notification-service
git commit -m "feat: add idempotent notification consumer"
```

### Task 9: Build `audit-service` as an Idempotent Append-Only Consumer

**Files:**
- Create: `services/audit-service/pom.xml`
- Create: `services/audit-service/src/main/java/com/btlbanking/audit/AuditServiceApplication.java`
- Create: `services/audit-service/src/main/java/com/btlbanking/audit/domain/AuditEventEntity.java`
- Create: `services/audit-service/src/main/java/com/btlbanking/audit/domain/ProcessedAuditEventEntity.java`
- Create: `services/audit-service/src/main/java/com/btlbanking/audit/kafka/TransferAuditConsumer.java`
- Test: `services/audit-service/src/test/java/com/btlbanking/audit/TransferAuditConsumerTest.java`

- [ ] **Step 1: Write the failing audit dedupe test**

```java
@SpringBootTest
class TransferAuditConsumerTest {
  @Autowired TransferAuditConsumer consumer;
  @Autowired AuditEventRepository auditEventRepository;

  @Test
  void duplicate_event_creates_one_audit_record() {
    var event = new TransferEvent("event-1", "TRANSFER_COMPENSATED", UUID.randomUUID(), "COMPENSATED");
    consumer.consume(event);
    consumer.consume(event);
    assertThat(auditEventRepository.count()).isEqualTo(1);
  }
}
```

- [ ] **Step 2: Run the audit consumer test to verify it fails**

Run: `mvn -q -pl services/audit-service -Dtest=TransferAuditConsumerTest test`
Expected: FAIL because no audit consumer exists yet.

- [ ] **Step 3: Implement the append-only audit consumer**

```java
@Component
@RequiredArgsConstructor
class TransferAuditConsumer {
  private final ProcessedAuditEventRepository processedRepository;
  private final AuditEventRepository auditRepository;

  @KafkaListener(topics = "transfer-events", groupId = "audit-service")
  @Transactional
  public void consume(TransferEvent event) {
    if (processedRepository.existsByEventId(event.eventId())) return;
    auditRepository.save(AuditEventEntity.from(event));
    processedRepository.save(new ProcessedAuditEventEntity(event.eventId(), event.transferId()));
  }
}
```

- [ ] **Step 4: Re-run the audit consumer test**

Run: `mvn -q -pl services/audit-service -Dtest=TransferAuditConsumerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/audit-service
git commit -m "feat: add idempotent audit consumer"
```

### Task 10: Wire Kong Gateway and the Gateway-Only JWT Validation Model

**Files:**
- Create: `infra/kong/kong.yml`

- Test: `scripts/smoke-auth-transfer.ps1`

- [ ] **Step 1: Write the failing gateway smoke script**

```powershell
# file: scripts/smoke-auth-transfer.ps1
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/auth/login -ContentType 'application/json' -Body '{"username":"alice","password":"secret123"}'
$token = $login.token
Invoke-WebRequest -Method Post -Uri http://localhost:8000/api/transfers -Headers @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'smoke-1' } -ContentType 'application/json' -Body '{"sourceAccount":"100001","destinationAccount":"200001","amount":500}'
```

- [ ] **Step 2: Run the script to verify it fails before Kong is configured**

Run: `powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1`
Expected: FAIL because Kong routes and JWT plugin are not configured yet.

- [ ] **Step 3: Configure Kong routes and forwarded identity headers**

```yaml
_format_version: "3.0"
services:
  - name: auth-service
    url: http://auth-service:8080
    routes:
      - name: auth-route
        paths: ["/api/auth"]
  - name: transaction-service
    url: http://transaction-service:8080
    routes:
      - name: transfer-route
        paths: ["/api/transfers"]
plugins:
  - name: jwt
    service: transaction-service
  - name: request-transformer
    service: transaction-service
    config:
      add:
        headers:
          - "X-User-Id:$(authenticated_jwt.claims.sub)"
```

- [ ] **Step 4: Validate Kong configuration**

Run: `docker run --rm -v ${PWD}/infra/kong:/kong kong:3.7 kong config parse /kong/kong.yml`
Expected: Kong declarative config parses successfully and includes JWT plus `X-User-Id` forwarding.

- [ ] **Step 5: Commit**

```bash
git add infra/kong/kong.yml scripts/smoke-auth-transfer.ps1
git commit -m "feat: add kong gateway routing and jwt enforcement"
```

### Task 11: Build the Static Frontend for Login, Transfer Submit, and Status Lookup

**Files:**
- Create: `frontend/index.html`
- Create: `frontend/app.js`
- Create: `frontend/styles.css`

- [ ] **Step 1: Write the minimal page markup**

```html
<!-- file: frontend/index.html -->
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Mini Banking Transfer</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <main>
    <form id="login-form"></form>
    <form id="transfer-form"></form>
    <section id="result"></section>
  </main>
  <script src="app.js"></script>
</body>
</html>
```

- [ ] **Step 2: Implement the browser logic**

```javascript
const state = { token: null, transferId: null };

async function login(username, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const data = await response.json();
  state.token = data.token;
}

async function submitTransfer(payload) {
  const response = await fetch('/api/transfers', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${state.token}`,
      'Idempotency-Key': crypto.randomUUID()
    },
    body: JSON.stringify(payload)
  });
  const data = await response.json();
  state.transferId = data.transferId;
  document.querySelector('#result').textContent = `${data.status}: ${data.message}`;
}
```

- [ ] **Step 3: Add minimal styling and status display**

```css
body { font-family: Arial, sans-serif; background: #f5f7fb; margin: 0; }
main { max-width: 640px; margin: 48px auto; background: white; padding: 24px; border-radius: 16px; }
input, button { width: 100%; margin: 8px 0; padding: 12px; }
#result { margin-top: 16px; font-weight: 700; }
```

- [ ] **Step 4: Smoke-test the static frontend**

Run: `python -m http.server 5500 -d frontend`
Expected: `Serving HTTP on 0.0.0.0 port 5500` and the page opens with login + transfer forms.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat: add banking transfer frontend"
```
### Task 12: Add Local Infrastructure, Topic Creation, and Observability Stack

**Files:**
- Create: `infra/docker-compose.yml`
- Create: `infra/postgres/init/00-create-databases.sql`
- Create: `infra/kafka/create-topics.sh`
- Create: `infra/otel/otel-collector-config.yaml`
- Create: `infra/prometheus/prometheus.yml`
- Create: `infra/filebeat/filebeat.yml`
- Create: `infra/grafana/provisioning/datasources/datasource.yml`
- Create: `infra/grafana/provisioning/dashboards/dashboard.yml`

- [ ] **Step 1: Write the Compose stack**

```yaml
# file: infra/docker-compose.yml
services:
  kong:
    image: kong:3.7
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.1
  kafka:
    image: confluentinc/cp-kafka:7.6.1
  redis:
    image: redis:7
  postgres:
    image: postgres:16
  jaeger:
    image: jaegertracing/all-in-one:1.57
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.107.0
  prometheus:
    image: prom/prometheus:v2.54.1
  grafana:
    image: grafana/grafana:11.2.0
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.0
  filebeat:
    image: docker.elastic.co/beats/filebeat:8.15.0
```

- [ ] **Step 2: Add database and topic bootstrap files**

```sql
CREATE DATABASE auth_db;
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
CREATE DATABASE notification_db;
CREATE DATABASE audit_db;
```

```bash
#!/usr/bin/env bash
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic transfer-events --partitions 3 --replication-factor 1
```

- [ ] **Step 3: Add telemetry and log collection configs**

```yaml
receivers:
  otlp:
    protocols:
      grpc:
      http:
exporters:
  jaeger:
    endpoint: jaeger:14250
  prometheus:
    endpoint: 0.0.0.0:9464
service:
  pipelines:
    traces: { receivers: [otlp], exporters: [jaeger] }
    metrics: { receivers: [otlp], exporters: [prometheus] }
```

```yaml
filebeat.inputs:
  - type: container
    paths: [/var/lib/docker/containers/*/*.log]
output.elasticsearch:
  hosts: ["http://elasticsearch:9200"]
```

- [ ] **Step 4: Validate the infrastructure syntax**

Run: `docker run --rm -v ${PWD}/infra/kong:/kong kong:3.7 kong config parse /kong/kong.yml`
Expected: fully rendered Compose config without syntax errors.

- [ ] **Step 5: Commit**

```bash
git add infra
git commit -m "feat: add local infra and observability stack"
```

### Task 13: Seed Demo Data and Add End-to-End Verification

**Files:**
- Create: `scripts/seed-demo-data.ps1`
- Modify: `scripts/smoke-auth-transfer.ps1`
- Modify: `README.md`

- [ ] **Step 1: Write the demo seed script**

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/auth/register -ContentType 'application/json' -Body '{"username":"alice","password":"secret123"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8082/accounts -ContentType 'application/json' -Body '{"accountNumber":"100001","ownerName":"Alice","balance":1000000}'
Invoke-RestMethod -Method Post -Uri http://localhost:8082/accounts -ContentType 'application/json' -Body '{"accountNumber":"200001","ownerName":"Bob","balance":500000}'
```

- [ ] **Step 2: Extend the smoke test to cover query-by-id and duplicate idempotency**

```powershell
$headers = @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'smoke-duplicate-1' }
$first = Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/transfers -Headers $headers -ContentType 'application/json' -Body '{"sourceAccount":"100001","destinationAccount":"200001","amount":500}'
$second = Invoke-RestMethod -Method Post -Uri http://localhost:8000/api/transfers -Headers $headers -ContentType 'application/json' -Body '{"sourceAccount":"100001","destinationAccount":"200001","amount":500}'
if ($first.transferId -ne $second.transferId) { throw 'Idempotency failed' }
Invoke-RestMethod -Method Get -Uri "http://localhost:8000/api/transfers/$($first.transferId)" -Headers @{ Authorization = "Bearer $token" }
```

- [ ] **Step 3: Document the full demo flow in the README**

```markdown
1. Start infrastructure with `docker compose -f infra/docker-compose.yml up -d`.
2. Start each service in its own terminal with commands such as `mvn -q -pl services/auth-service spring-boot:run` and `mvn -q -pl services/transaction-service spring-boot:run`.
3. Run `powershell -ExecutionPolicy Bypass -File scripts/seed-demo-data.ps1`.
4. Run `powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1`.
5. Open Jaeger, Grafana, Kibana, and the frontend page for the live demo.
```

- [ ] **Step 4: Run the final verification suite**

Run: `mvn -q test`
Expected: all module tests pass.

Run: `powershell -ExecutionPolicy Bypass -File scripts/smoke-auth-transfer.ps1`
Expected: register/login/transfer/query succeed and duplicate idempotency returns the same transfer id.

- [ ] **Step 5: Commit**

```bash
git add scripts README.md
git commit -m "test: add demo seed and smoke verification"
```

## Self-Review

- Spec coverage check:
  - JWT and gateway-only auth model are implemented in Tasks 2 and 10.
  - Account CRUD, debit, credit, and compensation are implemented in Task 3.
  - Fraud rule rejection is implemented in Task 4.
  - Transfer statuses and query endpoint are implemented in Task 5.
  - Saga orchestration and circuit breakers for fraud and account dependencies are implemented in Task 6.
  - Redis idempotency, outbox, single-topic Kafka events, and event types are implemented in Task 7.
  - Idempotent notification and audit consumers are implemented in Tasks 8 and 9.
  - Frontend, Docker Compose, observability, and demo flow are implemented in Tasks 11, 12, and 13.
- Placeholder scan: no `TODO`, `TBD`, or deferred steps remain.
- Type consistency check:
  - Transfer statuses use `PENDING`, `SUCCESS`, `FAILED`, `COMPENSATED`, and `REJECTED` throughout.
  - Event types use `TRANSFER_COMPLETED`, `TRANSFER_FAILED`, `TRANSFER_COMPENSATED`, and `TRANSFER_REJECTED` throughout.
  - Identity flows through `X-User-Id` consistently from Kong to `transaction-service`.


