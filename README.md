<div align="center">

# 🏗️ SemiColon — 건설현장 안전관리 대시보드 (Backend)

건설현장 작업자의 생체·환경 데이터를 실시간으로 수집하고,<br/>
위험을 감지해 알림·사고·긴급조치로 이어지는 안전관리 관제 시스템의 백엔드입니다.

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-59666C?style=flat-square&logo=hibernate&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A?style=flat-square&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/springdoc--openapi-85EA2D?style=flat-square&logo=swagger&logoColor=black)

</div>

---

## 📖 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [문서](#-문서)
- [시작하기](#-시작하기)
- [프로젝트 구조](#-프로젝트-구조)
- [API 규약](#-api-규약)
- [도메인 모델](#-도메인-모델)
- [개발 컨벤션](#-개발-컨벤션)

---

## 📌 프로젝트 소개

건설현장은 사고 위험이 높지만, 현장 상황을 한눈에 파악할 수 있는 수단이 부족합니다.
이 프로젝트는 작업자가 착용한 **웨어러블 디바이스**와 구역에 설치된 **환경 센서**에서
데이터를 모아, 관리자가 **하나의 대시보드에서 현장 전체의 안전 상태를 실시간으로 파악**하고
위험 상황에 즉시 대응할 수 있도록 하는 백엔드 시스템입니다.

```
[웨어러블 디바이스]  심박 / 체온 / 걸음수  ─┐
                                          ├─▶  수집  ─▶  임계값 판정  ─▶  알림(Alert)
[환경 센서]  온도 / 습도 / 미세먼지 / 산소 ─┘                              │
   / 화학물질 / 소음                                                       ▼
                                                        관리자 확인 ─▶ 사고 접수(Incident)
                                                                          │
                                                                          ▼
                                                     긴급조치(비상호출 / 방송 / 공정중단)
```

---

## ✨ 주요 기능

| 영역 | 설명 |
|---|---|
| 🔐 **인증/권한** | JWT 기반 관리자 로그인, `ADMIN` / `MANAGER` 역할 분리 |
| 🏭 **현장 관리** | 공정(Process) → 구역(Zone) → 작업자(Worker) 계층 관리, 구역 좌표 기반 맵 표시 |
| ⌚ **생체 모니터링** | 웨어러블 디바이스 배정·상태 관리, 심박/체온/걸음수 수집 및 실시간 조회 |
| 🌡️ **환경 모니터링** | 센서별 임계값(min/max) 설정, 측정값 수집 및 구역별 실시간 현황 |
| 🚨 **알림** | 위험 감지 시 등급별(긴급/위험/알림) 알림 생성, 대상 작업자 매핑, 확인 처리 |
| 🩹 **사고 관리** | 알림 연계 또는 수동 사고 접수, 처리 상태 추적(OPEN → IN_PROGRESS → RESOLVED) |
| 🆘 **긴급 조치** | 비상호출 / 안전방송 / 공정중단 실행 및 감사 로그 기록 |
| 🕘 **근태** | 출퇴근 기록, 일별 출근 현황 집계 |
| 🤖 **AI 인사이트** | 수집 데이터 기반 환경 변화·생체 위험·인력 배치 인사이트 생성 |
| 📊 **대시보드** | 관리자별 위젯 커스터마이징, 종합 요약 및 구역별 실시간 현황판 |

---

## 🛠 기술 스택

| 구분 | 사용 기술 |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 4.1.1 |
| **Build** | Gradle 9.7.1 (Groovy DSL) |
| **Database** | MySQL |
| **ORM** | Spring Data JPA (Hibernate) |
| **Security** | Spring Security, JWT |
| **Validation** | Jakarta Bean Validation |
| **API Docs** | springdoc-openapi 3.1.0 (Swagger UI) |
| **AI** | Spring AI 2.0.1 |
| **Etc** | Lombok |

> ⚠️ Spring Boot **4.x** 기준입니다. web starter 이름이 `spring-boot-starter-webmvc` 로,
> 테스트 starter가 모듈별(`spring-boot-starter-data-jpa-test` 등)로 분리되었습니다.
> 3.x 기준 예제 코드를 그대로 가져오면 동작하지 않습니다.

---

## 📚 문서

| 문서 | 링크 |
|---|---|
| API 명세서 | [Notion](https://app.notion.com/p/3cd2c89ec2fc8182be7cf312d1ca22ce) |
| ERD | [ERDCloud](https://www.erdcloud.com/d/NCF6XaSXn87sts3oW) |
| 개발 가이드 (아키텍처·컨벤션 상세) | [`CLAUDE.md`](./CLAUDE.md) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` (실행 후) |

---

## 🚀 시작하기

### 요구 사항

- JDK 21
- MySQL 8.0+

### 1. 데이터베이스 준비

```sql
CREATE DATABASE semicolon_safety
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 2. 로컬 설정 파일 작성

템플릿을 복사한 뒤 본인 환경에 맞게 값을 채웁니다.
**복사본은 `.gitignore` 에 등록되어 있으므로 커밋되지 않습니다.**

```powershell
cd src\main\resources
copy application-local.properties.example application-local.properties
```

채워야 할 값:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/semicolon_safety?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

jwt.secret=YOUR_BASE64_ENCODED_SECRET_AT_LEAST_32_BYTES
jwt.access-token-validity=1800000
jwt.refresh-token-validity=1209600000
```

### 3. 실행

```bash
./gradlew bootRun
```

> `local` 이 기본 프로파일로 지정되어 있어 별도 옵션 없이 실행됩니다.
> 테스트는 Gradle 이 `test` 프로파일을 강제하므로 인메모리 H2 로 돌아갑니다 — DB 없이도 `./gradlew test` 가 통과합니다.

### 4. 기타 명령어

```bash
./gradlew build      # 빌드 + 전체 테스트
./gradlew test       # 테스트만 실행
./gradlew clean      # 빌드 산출물 정리
```

---

## 📂 프로젝트 구조

도메인을 먼저 나누고, 그 안에서 계층을 나누는 **도메인형 패키지 구조**를 사용합니다.

```
src/main/java/com/example/be
├── BeApplication.java
├── global/                     # 도메인 공통 (횡단 관심사)
│   ├── common/                 # ApiResponse, PageResponse
│   ├── config/                 # Security, Swagger, JPA, CORS
│   ├── entity/                 # BaseTimeEntity, BaseCreatedEntity
│   ├── exception/              # ErrorCode, BusinessException, GlobalExceptionHandler
│   └── security/               # JwtTokenProvider, JwtAuthenticationFilter
└── domain/
    ├── auth/                   ├── envsensor/
    ├── manager/                ├── envrecord/
    ├── process/                ├── alert/
    ├── zone/                   ├── incident/
    ├── worker/                 ├── emergencyaction/
    ├── attendance/             ├── aiinsight/
    ├── wearabledevice/         └── dashboard/
    └── vitalrecord/
```

각 도메인 패키지는 아래 형태를 따릅니다.

```
domain/worker/
├── controller/     # HTTP 요청/응답만 담당
├── service/        # 비즈니스 로직, 트랜잭션 경계
├── repository/     # Spring Data JPA
├── entity/         # JPA 엔티티 + enum
└── dto/
    ├── request/
    └── response/
```

---

## 📡 API 규약

- **Base URL** : `/api`
- **인증** : `Authorization: Bearer {accessToken}`
- **페이징** : `page`(0-base), `size`(기본 20), `sort`(예: `createdAt,desc`)

### 응답 포맷

```json
// 성공
{ "success": true, "data": { "workerId": 1, "name": "김안전" }, "message": null, "code": null }

// 실패
{ "success": false, "data": null, "message": "존재하지 않는 작업자입니다.", "code": "WORKER_NOT_FOUND" }

// 검증 실패 - data 에 어떤 필드가 왜 틀렸는지 담긴다
{
  "success": false,
  "data": [
    { "field": "employeeNo", "rejectedValue": null, "reason": "사번은 필수입니다." }
  ],
  "message": "입력값이 올바르지 않습니다.",
  "code": "INVALID_INPUT_VALUE"
}
```

### 상태 코드

| 코드 | 상황 |
|---|---|
| `200` | 조회 / 수정 성공 |
| `201` | 생성 성공 |
| `204` | 삭제 성공 (본문 없음) |
| `400` | 요청 값 검증 실패 |
| `401` | 인증 실패 |
| `403` | 권한 부족 |
| `404` | 리소스 없음 |
| `409` | 중복 (사번, 로그인 ID, 시리얼 번호 등) |

### 도메인별 엔드포인트

| 도메인 | Base Path |
|---|---|
| 인증 | `/api/auth` |
| 관리자 | `/api/managers` |
| 공정 | `/api/processes` |
| 구역 | `/api/zones` |
| 작업자 | `/api/workers` |
| 근태 | `/api/attendances` |
| 웨어러블 디바이스 | `/api/wearable-devices` |
| 생체 기록 | `/api/vital-records` |
| 환경 센서 | `/api/env-sensors` |
| 환경 기록 | `/api/env-records` |
| 알림 | `/api/alerts` |
| 사고 | `/api/incidents` |
| 긴급 조치 | `/api/emergency-actions` |
| AI 인사이트 | `/api/ai-insights` |
| 대시보드 위젯 | `/api/dashboard-widgets` |
| 대시보드 집계 | `/api/dashboard` |

> 전체 엔드포인트 목록과 파라미터는 [API 명세서](https://app.notion.com/p/3cd2c89ec2fc8182be7cf312d1ca22ce)를 참고하세요.

---

## 🗂 도메인 모델

총 **15개 테이블**로 구성되어 있습니다.

```
processes ──1:N── zones ──1:N── workers ──1:N── attendances
    │                 │             │
    │                 │             └──1:N── wearable_devices ──1:N── vital_records
    │                 │
    │                 ├──1:N── env_sensors ──1:N── env_records
    │                 ├──1:N── alerts ──1:N── alert_workers ──N:1── workers
    │                 └──1:N── incidents ──N:1── alerts
    │
    └──1:N── ai_insights

managers ──1:N── emergency_actions ──N:1── processes
managers ──1:N── dashboard_widgets
```

<details>
<summary><b>주요 enum 값 펼쳐보기</b></summary>

| 대상 | 값 |
|---|---|
| `managers.role` | `ADMIN` / `MANAGER` |
| `managers.is_active` | 재직 여부. 삭제 대신 비활성 처리 (ERD 확장 컬럼) |
| `processes.status` | `RUNNING` / `STOPPED` |
| `workers.safety_status` | `NORMAL` / `CAUTION` / `DANGER` / `EMERGENCY` |
| `workers.employee_no` | 사번. 유니크 제약 추가 (ERD 확장) |
| `wearable_devices.device_type` | `BAND` / `HELMET` / `TAG` |
| `wearable_devices.connection_status` | `CONNECTED` / `DISCONNECTED` / `ERROR` |
| `env_sensors.sensor_type` | `TEMPERATURE` / `HUMIDITY` / `FINE_DUST` / `OXYGEN` / `CHEMICAL` / `NOISE` |
| `alerts.level` | `EMERGENCY` / `DANGER` / `NOTICE` |
| `alerts.alert_type` | `HEAT_RISK` / `COLLISION` / `JOINT_CARE` / `FALL` / `HEART_RATE_ANOMALY` / `STAMINA_CARE` / `RESOLVED` |
| `incidents.status` | `OPEN` / `IN_PROGRESS` / `RESOLVED` |
| `emergency_actions.action_type` | `EMERGENCY_CALL` / `BROADCAST` / `PROCESS_STOP` |
| `ai_insights.insight_type` | `ENV_CHANGE` / `VITAL_RISK` / `WORKFORCE_RECOMMEND` |
| `dashboard_widgets.widget_type` | `OVERALL_STATUS` / `LIVE_FEED` / `ENV_STATUS` / `WORKFORCE` / `WEARABLE_STATUS` / `DRIVEN_INSIGHT` |

</details>

전체 컬럼 정의는 [`CLAUDE.md` 6절](./CLAUDE.md) 또는 [ERD](https://www.erdcloud.com/d/NCF6XaSXn87sts3oW)를 참고하세요.

---

## 🤝 개발 컨벤션

### 브랜치 전략

```
main                  배포 가능한 안정 상태
└── develop           통합 브랜치
    ├── feat/{도메인}-{작업}     예: feat/worker-crud
    ├── fix/{이슈}              예: fix/jwt-expiration
    ├── refactor/{대상}
    └── chore/{작업}
```

### 커밋 메시지

```
{type}: {한 줄 요약}
```

| type | 용도 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 (동작 변화 없음) |
| `chore` | 빌드 / 설정 / 의존성 |
| `docs` | 문서 |
| `test` | 테스트 |

예시: `feat: 작업자 안전상태 변경 API 구현`

### Pull Request

- 대상 브랜치는 `develop`
- **하나의 PR = 하나의 도메인/기능** (여러 도메인을 섞지 않습니다)
- 리뷰어 1명 이상 승인 후 머지
- API를 추가·변경하면 **Notion API 명세서의 `개발 상황` / `담당자` 컬럼을 갱신**합니다

### 코드 스타일

| 대상 | 규칙 | 예시 |
|---|---|---|
| URL | kebab-case, 복수형 | `/api/wearable-devices/{deviceId}` |
| 클래스 | PascalCase | `WearableDeviceService` |
| 메서드 / 필드 | camelCase | `findByConnectionStatus` |
| enum 상수 | UPPER_SNAKE_CASE | `HEART_RATE_ANOMALY` |
| DTO | `{도메인}{동작}Request` / `{도메인}{용도}Response` | `WorkerCreateRequest` |

- 엔티티에 `@Setter` 를 붙이지 않습니다. 상태 변경은 의미 있는 메서드로 표현합니다.
- 모든 연관관계는 `FetchType.LAZY` 를 명시합니다.
- 엔티티를 Controller 밖으로 노출하지 않습니다. 응답은 항상 Response DTO입니다.

> 상세한 규칙은 [`CLAUDE.md`](./CLAUDE.md) 를 참고하세요.
