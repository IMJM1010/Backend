# CLAUDE.md — SemiColon 건설현장 안전관리 대시보드 Backend

이 파일은 Claude Code가 이 저장소에서 작업할 때 따라야 할 규칙과 도메인 지식을 담는다.

---

## 1. 프로젝트 개요

건설현장의 **작업자 안전**을 실시간으로 모니터링하는 관제 대시보드의 백엔드.

- 작업자가 착용한 **웨어러블 디바이스**에서 생체 데이터(심박/체온/걸음수)를 수집
- 구역에 설치된 **환경 센서**에서 온도/습도/미세먼지/산소/화학물질/소음을 수집
- 임계값 초과 시 **알림(alert)** 생성 → 관리자가 확인 → 필요 시 **사고(incident)** 접수 및 **긴급조치(emergency_action)** 실행
- 수집 데이터를 기반으로 **AI 인사이트** 제공
- 관리자별 **대시보드 위젯** 커스터마이징

주 사용자는 **관리자(managers)** 이며, 인증도 관리자 기준이다. 작업자(workers)는 로그인 주체가 아니라 관리 대상이다.

### 참고 문서
- API 명세서 (Notion): https://app.notion.com/p/3cd2c89ec2fc8182be7cf312d1ca22ce
- ERD (ERDCloud): https://www.erdcloud.com/d/NCF6XaSXn87sts3oW

---

## 2. 기술 스택

| 항목 | 값 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build | Gradle 9.7.1 (Groovy DSL, `./gradlew`) |
| DB | MySQL (`com.mysql:mysql-connector-j`) |
| ORM | Spring Data JPA |
| Security | Spring Security + JWT |
| Validation | Spring Boot Starter Validation |
| API Docs | springdoc-openapi 3.1.0 (`/swagger-ui.html`) |
| AI | Spring AI 2.0.1 (BOM) |
| Etc | Lombok |

> Spring Boot 4 부터 web starter 이름이 `spring-boot-starter-webmvc` 로, 테스트 starter가 모듈별
> (`spring-boot-starter-data-jpa-test` 등)로 분리되었다. 3.x 기준 코드/설정을 그대로 가져오지 말 것.

**Boot 4 로 오면서 달라진 것 중 이미 걸렸던 것들**
- **Jackson 3 사용** (`tools.jackson.*`, 기존 `com.fasterxml.jackson.*` 아님).
  `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` 가 제거되어
  `spring.jackson.serialization.write-dates-as-timestamps` 를 쓰면 **기동 실패**한다.
  Jackson 3 는 java.time 을 기본으로 ISO-8601 문자열로 직렬화하므로 설정 자체가 불필요하다.
- `@SpringBootTest` 가 MockMvc / WebTestClient / TestRestTemplate 을 자동 제공하지 않는다.
  컨트롤러 테스트에는 `@AutoConfigureMockMvc` 를 명시할 것.
- 설정 프로퍼티를 3.x 블로그 글에서 복사해 오지 말고, 의심되면
  [Boot 4.1 API 문서](https://docs.spring.io/spring-boot/4.1/api/java/)에서 해당 Properties 클래스를 먼저 확인할 것.

### 빌드 / 실행
```bash
./gradlew build          # 전체 빌드 + 테스트
./gradlew bootRun        # 애플리케이션 실행
./gradlew test           # 테스트만
./gradlew test --tests "com.example.be.domain.worker.*"
```

### 현재 상태 (2026-08-31)
- **1단계 기반 세팅 완료.** `global/` 패키지에 공통 응답·예외·설정·엔티티 베이스가 들어가 있다.
  - `global/common` — `ApiResponse`, `PageResponse`
  - `global/exception` — `ErrorCode`, `BusinessException`, `ValidationError`, `GlobalExceptionHandler`
  - `global/entity` — `BaseTimeEntity`, `BaseCreatedEntity`
  - `global/config` — `JpaConfig`, `SecurityConfig`(임시 permitAll), `CorsConfig`, `SwaggerConfig`
- 도메인 구현체는 아직 없음. 다음은 2단계(인증).
- 실행 프로파일은 `local` 이 기본. DB 접속 정보는 `application-local.properties`(gitignore 대상)에 각자 작성.
- 테스트는 Gradle 이 `test` 프로파일을 강제하여 인메모리 H2 로 실행된다.
  테스트 클래스에 `@ActiveProfiles` 를 따로 붙이지 않아도 된다.
- **`SecurityConfig` 는 전체 허용 상태다.** 2단계에서 JWT 필터와 인가 규칙으로 교체할 것.

### 착수 전 정리가 필요한 의존성 이슈
- `spring-ai-starter-vector-store-s3` 가 들어 있으나 AI 인사이트 생성에는 **채팅 모델 starter**
  (`spring-ai-starter-model-openai` 등)가 필요하다. 벡터 스토어가 실제로 필요한지 확인 후 교체/추가할 것.
- **JWT 라이브러리 미포함**. `io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson` 추가 필요.
- DB 스키마 관리 도구(Flyway/Liquibase) 미포함. 팀 합의 후 도입 권장.

---

## 3. 패키지 구조 (도메인형)

**도메인 우선 → 계층 하위** 구조를 따른다. 도메인 경계는 API 명세서의 도메인 구분과 1:1로 맞춘다.

```
com.example.be
├── BeApplication.java
├── global/                      # 도메인 공통 (횡단 관심사)
│   ├── common/
│   │   ├── ApiResponse.java     # { success, data, message, code }
│   │   └── PageResponse.java    # 페이징 응답 래퍼
│   ├── config/                  # SecurityConfig, SwaggerConfig, JpaConfig, CorsConfig
│   ├── entity/
│   │   ├── BaseTimeEntity.java     # created_at + updated_at (마스터성 테이블)
│   │   └── BaseCreatedEntity.java  # created_at 만 (로그성 테이블)
│   ├── exception/               # BusinessException, ErrorCode, GlobalExceptionHandler
│   └── security/                # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetails
└── domain/
    ├── auth/
    ├── manager/
    ├── process/
    ├── zone/
    ├── worker/
    ├── attendance/
    ├── wearabledevice/
    ├── vitalrecord/
    ├── envsensor/
    ├── envrecord/
    ├── alert/                   # alert_workers 매핑도 여기서 관리
    ├── incident/
    ├── emergencyaction/
    ├── aiinsight/
    └── dashboard/               # dashboard_widgets + /api/dashboard/* 집계
```

각 도메인 패키지 내부:

```
domain/worker/
├── controller/WorkerController.java
├── service/WorkerService.java
├── repository/WorkerRepository.java
├── entity/Worker.java
├── entity/SafetyStatus.java          # enum
└── dto/
    ├── request/WorkerCreateRequest.java
    ├── request/WorkerUpdateRequest.java
    └── response/WorkerResponse.java
```

### 계층 규칙
- **Controller**: HTTP 만 담당. 요청 검증(`@Valid`), DTO ↔ Service 위임, `ApiResponse` 로 감싸 반환. 비즈니스 로직 금지.
- **Service**: 트랜잭션 경계. 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.
- **Repository**: Spring Data JPA 인터페이스. 복잡한 조회는 `@Query` 또는 QueryDSL(도입 시).
- **Entity는 절대 Controller 밖으로 노출하지 않는다.** 응답은 항상 Response DTO.
- 도메인 간 참조는 **Service → 다른 도메인의 Service** 를 통한다. 다른 도메인의 Repository 직접 호출 금지.

---

## 4. API 공통 규약

- Base URL: `/api`
- 인증: `Authorization: Bearer {accessToken}` (JWT)
- 응답 포맷:

```json
{ "success": true, "data": { }, "message": null, "code": null }
```
```json
{ "success": false, "data": null, "message": "존재하지 않는 작업자입니다.", "code": "WORKER_NOT_FOUND" }
```

- `code` 는 에러 식별용이며 `ErrorCode` enum 상수 이름이 그대로 내려간다. 프론트가 이 값으로 분기하므로
  **이미 배포된 상수의 이름을 바꾸면 프론트가 깨진다.** 이름 변경 대신 새 상수를 추가할 것.
- 검증 실패(400)는 `data` 에 필드별 상세를 담아 내려준다:
  `[{ "field": "employeeNo", "rejectedValue": null, "reason": "사번은 필수입니다." }]`
- 목록 조회 공통 파라미터: `page`(0-base), `size`(기본 20), `sort`(예: `createdAt,desc`)
- HTTP 상태 코드
  | 상황 | 코드 |
  |---|---|
  | 조회/수정 성공 | 200 |
  | 생성 성공 | 201 |
  | 삭제 성공(본문 없음) | 204 |
  | 검증 실패 / 잘못된 파라미터 | 400 |
  | 인증 실패 | 401 |
  | 권한 부족 | 403 |
  | 리소스 없음 | 404 |
  | 중복(사번, login_id, serial_no 등) | 409 |

- 모든 예외는 `GlobalExceptionHandler`(`@RestControllerAdvice`)에서 `ApiResponse` 로 변환한다.
  Controller/Service 안에서 `try-catch` 로 응답을 직접 만들지 않는다.
- 도메인별 에러는 `ErrorCode` enum(코드 + 메시지 + HttpStatus)으로 정의하고 `BusinessException` 으로 던진다.

### 날짜/시간
- Java 타입: `LocalDate`(work_date), `LocalDateTime`(그 외 전부)
- JSON 직렬화: ISO-8601 (`2026-08-31T14:30:00`)
- 서버 타임존: `Asia/Seoul`

---

## 5. 인증 / 권한 정책

- 로그인 주체: `managers` (`login_id` + `password`)
- 비밀번호: `BCryptPasswordEncoder` 로 해싱하여 `managers.password`(VARCHAR(255)) 에 저장. **평문 저장 금지.**
- Access Token(짧게, 예: 30분) + Refresh Token(길게, 예: 14일) 구조.
- `role` 은 `ADMIN` / `MANAGER` 두 가지.
  - `ADMIN`: 관리자 등록/삭제, 권한 변경, 공정·구역·센서 마스터 데이터 변경
  - `MANAGER`: 조회 전반, 알림 확인, 사고 접수/처리, 긴급조치 실행, 본인 위젯 설정
- 본인 리소스(`/api/managers/{id}`, `/api/managers/{id}/password`, 위젯 설정)는 **본인 또는 ADMIN** 만 접근.
- 인증 없이 접근 가능: `/api/auth/login`, `/api/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`
- **디바이스/센서 수집 API**(`POST /api/vital-records`, `POST /api/env-records`)는 사람이 아닌
  게이트웨이가 호출한다. JWT가 아닌 **API Key 인증 트랙**으로 분리하는 것을 전제로 설계할 것.

---

## 6. DB 스키마 (ERD 기준, 15개 테이블)

공통 규칙
- PK: `BIGINT AUTO_INCREMENT`, 이름은 `{단수형}_id`
- 모든 테이블: `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
- 마스터성 테이블: `updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
  (로그성 테이블 — `vital_records`, `env_records`, `alerts`, `alert_workers`, `ai_insights`,
  `emergency_actions` — 에는 `updated_at` 없음)
- enum 성격 컬럼은 DB에서 `VARCHAR`, JPA에서는 `@Enumerated(EnumType.STRING)` 으로 매핑

### managers (관리자)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| manager_id | BIGINT PK | |
| login_id | VARCHAR(50) NN | 로그인 ID, 유니크 |
| password | VARCHAR(255) NN | BCrypt 해시 |
| name | VARCHAR(30) NN | |
| role | VARCHAR(20) NN DEF 'MANAGER' | `ADMIN` / `MANAGER` |
| profile_image_url | VARCHAR(500) | |
| created_at / updated_at | DATETIME NN | |

### processes (공정)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| process_id | BIGINT PK | |
| name | VARCHAR(50) NN | 예: 허브공정 |
| status | VARCHAR(20) NN DEF 'RUNNING' | `RUNNING` / `STOPPED` |
| created_at / updated_at | DATETIME NN | |

### zones (구역)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| zone_id | BIGINT PK | |
| process_id | BIGINT NN → processes | |
| zone_code | VARCHAR(10) NN | A-1, B-3 등 |
| name | VARCHAR(50) | |
| map_x / map_y | DECIMAL(8,2) | 맵 표시용 좌표 |
| created_at / updated_at | DATETIME NN | |

### workers (작업자)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| worker_id | BIGINT PK | |
| zone_id | BIGINT NULL → zones | 현재 위치 구역 |
| employee_no | VARCHAR(30) NN | 사번, 유니크 |
| name | VARCHAR(30) NN | |
| safety_status | VARCHAR(20) NN DEF 'NORMAL' | `NORMAL`/`CAUTION`/`DANGER`/`EMERGENCY` |
| is_active | BOOLEAN NN DEF TRUE | 재직 여부 |
| created_at / updated_at | DATETIME NN | |

### attendances (근태)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| attendance_id | BIGINT PK | |
| worker_id | BIGINT NN → workers | |
| work_date | DATE NN | `(worker_id, work_date)` 유니크 권장 |
| check_in_at | DATETIME | |
| check_out_at | DATETIME | |
| created_at / updated_at | DATETIME NN | |

### wearable_devices (웨어러블 디바이스)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| device_id | BIGINT PK | |
| worker_id | BIGINT NULL → workers | 미배정 시 NULL |
| serial_no | VARCHAR(50) NN | 유니크 |
| device_type | VARCHAR(30) NN | `BAND` / `HELMET` / `TAG` |
| battery_level | TINYINT | 0~100 |
| connection_status | VARCHAR(20) NN DEF 'DISCONNECTED' | `CONNECTED`/`DISCONNECTED`/`ERROR` |
| last_synced_at | DATETIME | |
| created_at / updated_at | DATETIME NN | |

### vital_records (생체 기록) — 로그성, 대용량
| 컬럼 | 타입 | 비고 |
|---|---|---|
| vital_record_id | BIGINT PK | |
| device_id | BIGINT NN → wearable_devices | |
| heart_rate | SMALLINT | bpm |
| body_temp | DECIMAL(4,1) | ℃ |
| step_count | INT | 근무체력 판단용 |
| measured_at | DATETIME NN | 인덱스 `(device_id, measured_at DESC)` |
| created_at | DATETIME NN | |

### env_sensors (환경 센서)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| sensor_id | BIGINT PK | |
| zone_id | BIGINT NN → zones | |
| sensor_type | VARCHAR(20) NN | `TEMPERATURE`/`HUMIDITY`/`FINE_DUST`/`OXYGEN`/`CHEMICAL`/`NOISE` |
| unit | VARCHAR(10) NN | ℃, %, ㎍/㎥, dB |
| threshold_min / threshold_max | DECIMAL(10,2) | 정상 범위 |
| created_at / updated_at | DATETIME NN | |

### env_records (환경 기록) — 로그성, 대용량
| 컬럼 | 타입 | 비고 |
|---|---|---|
| env_record_id | BIGINT PK | |
| sensor_id | BIGINT NN → env_sensors | |
| value | DECIMAL(10,2) NN | |
| measured_at | DATETIME NN | 인덱스 `(sensor_id, measured_at DESC)` |
| created_at | DATETIME NN | |

### alerts (알림)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| alert_id | BIGINT PK | |
| zone_id | BIGINT NN → zones | 발생 구역 |
| level | VARCHAR(20) NN | `EMERGENCY`(긴급) / `DANGER`(위험) / `NOTICE`(알림) |
| alert_type | VARCHAR(30) NN | `HEAT_RISK`/`COLLISION`/`JOINT_CARE`/`FALL`/`HEART_RATE_ANOMALY`/`STAMINA_CARE`/`RESOLVED` |
| message | VARCHAR(200) NN | 표시 문구 (예: 고열위험 감지) |
| worker_count | INT NN DEF 1 | 대상 인원 수 |
| is_confirmed | BOOLEAN NN DEF FALSE | |
| confirmed_by | BIGINT NULL → managers | |
| confirmed_at | DATETIME | |
| occurred_at | DATETIME NN | |
| created_at | DATETIME NN | |

### alert_workers (알림-작업자 매핑)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| alert_worker_id | BIGINT PK | |
| alert_id | BIGINT NN → alerts | |
| worker_id | BIGINT NN → workers | `(alert_id, worker_id)` 유니크 |
| created_at | DATETIME NN | |

### incidents (사고)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| incident_id | BIGINT PK | |
| alert_id | BIGINT NULL → alerts | 근원 알림. NULL이면 수동 접수 |
| zone_id | BIGINT NN → zones | |
| incident_type | VARCHAR(30) NN | `COLLISION`/`FALL`/`HEAT_STROKE` 등 |
| description | TEXT | |
| status | VARCHAR(20) NN DEF 'OPEN' | `OPEN`/`IN_PROGRESS`/`RESOLVED` |
| handled_by | BIGINT NULL → managers | |
| occurred_at | DATETIME NN | |
| resolved_at | DATETIME | |
| created_at / updated_at | DATETIME NN | |

### emergency_actions (긴급 조치 로그) — 감사 로그
| 컬럼 | 타입 | 비고 |
|---|---|---|
| action_id | BIGINT PK | |
| manager_id | BIGINT NN → managers | 실행 관리자 |
| process_id | BIGINT NULL → processes | 공정 중단 시 대상 공정 |
| action_type | VARCHAR(30) NN | `EMERGENCY_CALL`/`BROADCAST`/`PROCESS_STOP` |
| executed_at | DATETIME NN DEF CURRENT_TIMESTAMP | |

### ai_insights (AI 인사이트)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| insight_id | BIGINT PK | |
| process_id | BIGINT NULL → processes | 관련 공정 |
| insight_type | VARCHAR(30) NN | `ENV_CHANGE`/`VITAL_RISK`/`WORKFORCE_RECOMMEND` 등 |
| content | TEXT NN | |
| created_at | DATETIME NN | |

### dashboard_widgets (대시보드 위젯)
| 컬럼 | 타입 | 비고 |
|---|---|---|
| widget_id | BIGINT PK | |
| manager_id | BIGINT NN → managers | |
| widget_type | VARCHAR(30) NN | `OVERALL_STATUS`/`LIVE_FEED`/`ENV_STATUS`/`WORKFORCE`/`WEARABLE_STATUS`/`DRIVEN_INSIGHT` |
| display_order | INT NN DEF 0 | |
| show_widget | BOOLEAN NN DEF TRUE | |
| created_at / updated_at | DATETIME NN | |

### 관계 요약
```
processes 1 ── N zones 1 ── N workers 1 ── N attendances
                     │             └─ N wearable_devices 1 ── N vital_records
                     ├── N env_sensors 1 ── N env_records
                     ├── N alerts ── N alert_workers ── workers
                     └── N incidents ── (alert_id) alerts
managers ── N emergency_actions ── (process_id) processes
managers ── N dashboard_widgets
processes 1 ── N ai_insights
```

---

## 7. 엔티티 작성 규칙

```java
@Entity
@Table(name = "workers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worker extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "worker_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "employee_no", nullable = false, length = 30, unique = true)
    private String employeeNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_status", nullable = false, length = 20)
    private SafetyStatus safetyStatus = SafetyStatus.NORMAL;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    // ...
}
```

지켜야 할 것
- `@Setter` 금지. 상태 변경은 의미 있는 메서드로 (`worker.moveTo(zone)`, `alert.confirm(manager)`).
- 기본 생성자는 `@NoArgsConstructor(access = PROTECTED)`, 생성은 `@Builder` 또는 정적 팩토리 메서드.
- **모든 연관관계는 `FetchType.LAZY`.** `@ManyToOne` 기본값이 EAGER이므로 반드시 명시.
- 양방향 연관관계는 꼭 필요할 때만. 기본은 단방향 `@ManyToOne`.
- `@ToString`, `@EqualsAndHashCode` 를 엔티티에 붙이지 않는다 (LAZY 프록시 순환 참조).
- `cascade = REMOVE` / `orphanRemoval` 은 신중히. 이력성 테이블은 삭제 전파 금지.
- 필드는 camelCase, 컬럼은 snake_case를 `@Column(name = ...)` 으로 **명시**한다 (네이밍 전략에 의존하지 않는다).

### 삭제 정책
- `emergency_actions`, `incidents`, `alerts` 는 감사/이력 성격 → **하드 삭제 지양**.
  API 명세에 DELETE가 있어도 소프트 삭제 또는 ADMIN 전용으로 제한한다.
- `workers` 삭제 = `is_active = false` (퇴사 처리). 실제 row 삭제 금지.
- `managers` 삭제 = 비활성 처리 권장.
- `processes` / `zones` 삭제는 하위 데이터 존재 시 409 로 거부한다.

---

## 8. 코드 컨벤션

### 네이밍
| 대상 | 규칙 | 예시 |
|---|---|---|
| URL | kebab-case, 복수형 | `/api/wearable-devices/{deviceId}/vital-records` |
| 패키지 | 소문자, 구분자 없음 | `com.example.be.domain.wearabledevice` |
| 클래스 | PascalCase | `WearableDeviceService` |
| 메서드/필드 | camelCase | `findByConnectionStatus` |
| enum 상수 | UPPER_SNAKE | `HEART_RATE_ANOMALY` |
| DTO | `{도메인}{동작}Request` / `{도메인}{용도}Response` | `WorkerCreateRequest`, `WorkerDetailResponse` |
| 테스트 | `{클래스명}Test` | `WorkerServiceTest` |

### Service 메서드 네이밍
- 조회 단건: `getXxx` (없으면 예외) / `findXxx` (Optional 반환)
- 조회 목록: `getXxxList`, `searchXxx`
- 생성: `createXxx` / 수정: `updateXxx` / 삭제: `deleteXxx`
- 상태 변경: `confirmAlert`, `checkIn`, `checkOut`, `changeSafetyStatus`

### DTO
- **record 사용** (Java 21). 요청 DTO에는 `@NotNull`, `@NotBlank`, `@Size`, `@Min` 등 Bean Validation 명시.
- Entity → Response 변환은 Response DTO의 정적 팩토리(`WorkerResponse.from(worker)`)에서 수행.

### 기타
- 들여쓰기 4칸, 한 줄 120자 이내.
- 주석은 "왜"를 적는다. 코드가 설명하는 "무엇"은 적지 않는다.
- `System.out.println` 금지. 로깅은 `@Slf4j` + SLF4J.
- 매직 넘버/문자열 금지 → enum 또는 상수.

---

## 9. 성능 / 주의사항

- **N+1 주의**: 목록 조회에서 연관 엔티티를 함께 쓰면 `@EntityGraph` 또는 fetch join 사용.
  특히 `alerts` 목록(zone, confirmedBy), `workers` 목록(zone)에서 발생하기 쉽다.
- `vital_records`, `env_records` 는 수집 주기에 따라 **초당 수십 건 이상** 쌓인다.
  - 전체 목록 조회는 반드시 기간 필터 + 페이징 강제.
  - "실시간 조회" API(`/realtime`)는 최신 1건만 가져오는 쿼리로 구현 (전체 정렬 금지).
  - 대량 INSERT는 batch insert 고려.
- 대시보드 집계(`/api/dashboard/summary`, `/api/dashboard/zones/realtime`)는 여러 테이블을 조인한다.
  N번의 단건 쿼리 대신 **집계 쿼리 1~2개**로 처리하고, 필요 시 캐싱을 검토.
- 알림 생성은 센서 임계값 초과 판정과 연결된다. 중복 알림 방지 로직(같은 zone/type의 미확인 알림 존재 시 스킵 등)을 서비스에 둘 것.
- 실시간성이 필요한 도메인(alerts, vital_records, env_records, workers.safety_status)은
  REST 폴링 외에 **WebSocket/SSE** 를 별도 트랙으로 검토한다. 초기 구현은 REST 우선.

---

## 10. API 목록 (도메인별)

> 전체 상세는 Notion API 명세서 참조. 아래는 구현 체크리스트.

**Auth** — `POST /api/auth/login`, `POST /api/auth/logout`, `POST /api/auth/refresh`, `GET /api/auth/me`

**Managers** — `GET|POST /api/managers`, `GET|PATCH|DELETE /api/managers/{id}`,
`PATCH /api/managers/{id}/password`, `POST /api/managers/{id}/profile-image`

**Processes** — `GET|POST /api/processes`, `GET|PATCH|DELETE /api/processes/{id}`,
`GET /api/processes/{id}/zones`, `GET /api/processes/{id}/ai-insights`

**Zones** — `GET|POST /api/zones`, `GET|PATCH|DELETE /api/zones/{id}`,
`GET /api/zones/{id}/workers`, `GET /api/zones/{id}/env-sensors`, `GET /api/zones/{id}/alerts`

**Workers** — `GET|POST /api/workers`, `GET|PATCH|DELETE /api/workers/{id}`,
`PATCH /api/workers/{id}/safety-status`, `PATCH /api/workers/{id}/zone`,
`GET /api/workers/{id}/attendances`, `GET /api/workers/{id}/wearable-devices`, `GET /api/workers/{id}/alerts`

**Attendances** — `GET /api/attendances`, `GET|PATCH|DELETE /api/attendances/{id}`,
`POST /api/attendances/check-in`, `PATCH /api/attendances/{id}/check-out`,
`GET /api/attendances/summary?date=`

**Wearable Devices** — `GET|POST /api/wearable-devices`, `GET|PATCH|DELETE /api/wearable-devices/{id}`,
`PATCH /api/wearable-devices/{id}/status`, `GET /api/wearable-devices/{id}/vital-records`

**Vital Records** — `GET|POST /api/vital-records`, `GET|DELETE /api/vital-records/{id}`,
`GET /api/vital-records/realtime?workerId=`

**Env Sensors** — `GET|POST /api/env-sensors`, `GET|PATCH|DELETE /api/env-sensors/{id}`,
`GET /api/env-sensors/{id}/records`

**Env Records** — `GET|POST /api/env-records`, `DELETE /api/env-records/{id}`,
`GET /api/env-records/realtime?zoneId=`

**Alerts** — `GET|POST /api/alerts`, `GET|DELETE /api/alerts/{id}`, `PATCH /api/alerts/{id}/confirm`,
`GET /api/alerts/unconfirmed`,
`GET|POST /api/alerts/{id}/workers`, `DELETE /api/alerts/{id}/workers/{workerId}`

**Incidents** — `GET|POST /api/incidents`, `GET|PATCH|DELETE /api/incidents/{id}`,
`PATCH /api/incidents/{id}/status`, `GET /api/incidents/{id}/emergency-actions`

**Emergency Actions** — `GET|POST /api/emergency-actions`, `GET|DELETE /api/emergency-actions/{id}`

**AI Insights** — `GET /api/ai-insights`, `GET|DELETE /api/ai-insights/{id}`,
`POST /api/ai-insights/generate`

**Dashboard Widgets** — `GET|POST /api/dashboard-widgets`, `PATCH|DELETE /api/dashboard-widgets/{id}`,
`PATCH /api/dashboard-widgets/reorder`

**Dashboard** — `GET /api/dashboard/summary`, `GET /api/dashboard/zones/realtime`

---

## 11. 개발 우선순위

의존 관계상 아래 순서를 권장한다.

1. **기반 세팅** — datasource 설정, `BaseTimeEntity` + JPA Auditing, `ApiResponse`,
   `ErrorCode` / `BusinessException` / `GlobalExceptionHandler`, Swagger, CORS
2. **인증** — `managers` 엔티티 → SecurityConfig + JWT(Provider/Filter) → Auth API → Managers API
3. **마스터 데이터** — Processes → Zones → Workers (여기까지 되면 화면 뼈대가 나옴)
4. **수집 계층** — Wearable Devices → Vital Records / Env Sensors → Env Records
5. **운영 계층** — Alerts (+ alert_workers) → Incidents → Emergency Actions
6. **부가 기능** — Attendances, Dashboard Widgets
7. **집계/AI** — `/api/dashboard/*` 집계 API → AI Insights
8. **확장** — WebSocket/SSE 실시간 푸시, 디바이스용 API Key 인증 트랙

---

## 12. 협업 규칙

### 브랜치
```
main            # 배포 가능한 상태
develop         # 통합 브랜치
feat/{도메인}-{작업}   # 예: feat/worker-crud
fix/{이슈}
refactor/{대상}
```

### 커밋 메시지
```
{type}: {한 줄 요약}

feat:     기능 추가
fix:      버그 수정
refactor: 리팩터링 (동작 변화 없음)
chore:    빌드/설정/의존성
docs:     문서
test:     테스트
```
예: `feat: 작업자 안전상태 변경 API 구현`

### PR
- `develop` 으로 보낸다. 리뷰어 1명 이상 승인 후 머지.
- 하나의 PR = 하나의 도메인/기능. 여러 도메인을 섞지 않는다.
- API를 추가·변경하면 **Notion API 명세서의 개발 상황/담당자 컬럼을 갱신**한다.

---

## 13. Claude 작업 지침

- **응답은 한국어로.** 코드 식별자와 기술 용어는 영어 그대로.
- 새 도메인을 만들 때는 위 패키지 구조·계층 규칙을 **그대로** 따른다. 임의로 구조를 바꾸지 않는다.
- 엔티티 필드/타입은 **6절 스키마를 기준**으로 한다. ERD에 없는 컬럼을 임의로 추가하지 않는다.
  필요하다고 판단되면 먼저 그 이유를 말하고 확인을 받는다.
- API의 URL·HTTP 메서드는 **10절 목록을 기준**으로 한다. 명세와 다르게 만들지 않는다.
- 구현 전에 기존 도메인 중 **가장 비슷한 것을 먼저 읽고** 같은 스타일로 작성한다.
- Spring Boot 4 / Java 21 기준으로 작성한다. 3.x 관용구(`WebSecurityConfigurerAdapter`,
  `javax.*` 패키지, 구버전 starter 이름)를 쓰지 않는다.
- 코드를 수정한 뒤에는 `./gradlew build` 로 컴파일이 통과하는지 확인한다.
- 여러 도메인을 한 번에 만들지 않는다. 한 도메인씩 완성하고 확인을 받는다.
