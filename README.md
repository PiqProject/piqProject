# 💘 PIQ  - Backend API Server

<div align="center">

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-Storage-569A31?style=flat-square&logo=amazons3&logoColor=white)
![PortOne](https://img.shields.io/badge/PortOne-V2_Payment-orange?style=flat-square)
![JMeter](https://img.shields.io/badge/Apache_JMeter-Benchmarked-D22128?style=flat-square&logo=apachejmeter&logoColor=white)

<br/>

**사용자 맞춤 프로필 추천과 포인트 결제 시스템을 제공하는 데이팅 웹 서비스 백엔드 시스템**  


</div>

---

## 📋 목차

1. [시스템 아키텍처 & 핵심 도메인 모델](#-시스템-아키텍처--핵심-도메인-모델)
2. [핵심 담당 역할 & 비즈니스 기능 구현](#-핵심-담당-역할--비즈니스-기능-구현)
3. [핵심 엔지니어링 & 성능 최적화 (Deep Dive)](#-핵심-엔지니어링--성능-최적화-deep-dive)
4. [기술적 트레이드오프 & 엔지니어링 회고](#%EF%B8%8F-기술적-트레이드오프--엔지니어링-회고)
5. [프로젝트 구조](#-프로젝트-구조)
6. [시작하기 (Getting Started)](#-시작하기)
7. [환경 변수 가이드](#-환경-변수-가이드)

---

## 🏛 시스템 아키텍처 & 핵심 도메인 모델

### System Architecture

클라이언트 요청 처리부터 트랜잭션 경계 분리 및 외부 I/O 통신을 체계적으로 격리한 계층형 아키텍처입니다.

```mermaid
flowchart TD
    Client["Client (Mobile / Web)"] --> Controller["Controller Layer (요청 라우팅 & 검증)"]

    subgraph SpringBoot ["Spring Boot Application Server"]
        Controller --> Facade["Facade Layer (S3 I/O 트랜잭션 격리)"]
        Facade --> Service["Service Layer (핵심 비즈니스 & 트랜잭션)"]
    end

    Facade -.->|"1. 사전 업로드 (커넥션 점유 0ms)"| S3["AWS S3 / Object Storage"]
    Service -->|"2. 영속화 & 배타락"| DB[("PostgreSQL")]
    Service -->|"3. 승인 검증 & 보상 취소"| PortOne["PortOne API (PG)"]
    Service -.->|"토큰 관리"| Redis[("Redis")]
```

### Core Domain Model (ERD)

```mermaid
erDiagram
    USER ||--o{ MATCHING : "sender / receiver"
    USER ||--o{ USER_IMAGE : "has"
    USER ||--o{ PAYMENT : "orders"
    PRODUCT ||--o{ PAYMENT : "included in"

    USER {
        bigint id PK
        string email
        string nickname
        int pq_point "보유 포인트"
    }

    MATCHING {
        bigint match_id PK
        bigint sender_id FK "발신자"
        bigint receiver_id FK "수신자"
        string status "매칭 상태"
        datetime created_at "정렬 인덱스"
    }

    USER_IMAGE {
        bigint image_id PK
        bigint user_id FK "사용자"
        string image_url
        boolean is_main_image
    }

    PAYMENT {
        bigint payment_id PK
        string merchant_uid "비관적 락 대상"
        string transaction_id "결제 고유번호"
        string status "READY / PAID"
        decimal amount "결제 금액"
    }

    PRODUCT {
        bigint product_id PK
        int price "결제 금액"
        int point "충전 포인트"
    }
```

---

## 🎯 핵심 담당 역할 & 비즈니스 기능 구현

<details open>
<summary><b>1. 매칭 및 추천 도메인 (Matching)</b></summary>

<br/>

- **일일 매칭 프로필 추천** — 사용자 성향 기반 맞춤 상대 프로필 2장 제공
- **양방향 매칭 라이프사이클** — 매칭 신청 → 수락/거절 상태 전이 및 중복 요청 차단
- **재화(포인트) 연동** — 매칭 신청 시 보유 포인트(`pq_point`) 유효성 검증 및 즉시 차감

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/matches` | 매칭 요청 발송 |
| `GET` | `/api/v1/matches/sent?page=0&size=20` | 보낸 매칭 페이징 조회 |
| `PATCH` | `/api/v1/matches/{id}/accept` | 매칭 수락 |

</details>

<details open>
<summary><b>2. 프로필 & 미디어 관리 도메인 (UserImage)</b></summary>

<br/>

- **다중 프로필 이미지 관리** — 사용자당 최대 4장 등록 제한 (`FILE_NUMBER_EXCEEDED`)
- **대표 사진 자동 승격(폴백)** — 대표 이미지 삭제 시 잔여 사진 중 1순위를 자동 대표로 승격
- **콘텐츠 검수 연계** — 업로드 시 관리자 검수 대기(`VerificationEntity`) 상태 발행

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/images/upload` | 멀티파트 이미지 업로드 |
| `DELETE` | `/api/v1/images/{id}` | 이미지 삭제 및 대표 이미지 자동 후처리 |
| `PATCH` | `/api/v1/images/{id}/main` | 대표 이미지 수동 변경 |

</details>

<details open>
<summary><b>3. 결제 & 포인트 충전 도메인 (Payment)</b></summary>

<br/>

- **포트원 V2 연동** — 고유 주문번호(`merchantUid`) 사전 발급 (`prepare`)
- **결제 사후 위변조 검증** — PG사 단건 조회를 통한 금액 검증 및 포인트 1회 충전 (`verify`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/payments/prepare` | 결제 사전 주문번호 발급 |
| `POST` | `/api/v1/payments/verify` | 결제 사후 검증 및 포인트 충전 |
| `POST` | `/api/v1/payments/cancel` | 환불 유효기간 검증 및 결제 취소 |

</details>

---

## ⚡ 핵심 엔지니어링 & 성능 최적화 (Deep Dive)

### 1. JPA N+1 문제 해결 및 네트워크 RTT 단축

> **대상 API:** `GET /api/v1/matches/sent?page=0&size=20` (보낸 매칭 목록 페이징)

#### 🚨 문제 정의

연관된 발신자(`sender`), 수신자(`receiver`) 및 이미지(`userImages`) 지연 로딩으로 **1회 요청당 43건의 N+1 쿼리 폭증**.  
쿼리 폭증으로 HikariCP 커넥션 풀(10개) 점유 시간이 길어져 **동시 50명 부하 시 평균 응답 416ms**까지 지연.

#### 💡 해결: 하이브리드 패치 전략

| 관계 유형 | 전략 | 근거 |
|-----------|------|------|
| `@ManyToOne` (ToOne) | **`JOIN FETCH`** 단일 쿼리 결합 | 카테시안 곱이 발생하지 않아 안전 |
| `@OneToMany` (컬렉션) | **`default_batch_fetch_size: 100`** | 메모리 OOM 방지, `IN` 절 1방으로 압축 |

#### 📊 성능 측정 결과

```
평균 응답 속도 (Avg Latency)
[개선 전] ████████████████████████████████████████  416 ms
[개선 후] ██                                        16 ms  ( 96.1% 단축)
```

| 측정 지표 | 개선 전 (순수 N+1) | 개선 후 (Fetch Join + Batch) | 개선 성과 |
|-----------|-------------------:|----------------------------:|-----------|
| 평균 응답 시간 | 416 ms | **16 ms** |  26배 향상 |
| 95% 응답 (p95) | 662 ms | **22 ms** |  30배 안정화 |
| 최대 지연 (Max) | 872 ms | **382 ms** | 56.2% 감소 |
| 초당 처리량 (TPS) | 61.3 TPS | **99.8 TPS** |  63% 증가 |

---

### 2. 복합 인덱스(Composite Index) 설계를 통한 DB Filesort 제거

> **대상:** 매칭 데이터 2,000건+ 누적 환경 최신순 페이징 API

#### 🚨 문제 정의

최신순 페이징(`WHERE sender_id = ? ORDER BY created_at DESC LIMIT 20`) 시,  
`sender_id` 단일 인덱스만 존재하여 매칭 2,000건을 메모리로 올려 퀵정렬을 수행하는 **Filesort 병목 발생**.  
동시 50명 부하 시 DB 정렬 큐 대기로 **최대 지연 753ms, 처리량 18.6 TPS로 급감**.

#### 💡 해결: 선두 컬럼 규칙 기반 복합 인덱스

동등 조건(`sender_id`)을 **선두**, 정렬 조건(`created_at DESC`)을 **후두**에 배치.  
B-Tree 리프 노드의 물리적 정렬 순서를 활용해 추가 정렬 비용을 **제거**하고 20건만 읽고 멈추는 **Early Exit** 유도.

```java
// MatchingEntity.java
@Table(name = "matching", indexes = {
    @Index(name = "idx_matching_sender_created",
           columnList = "sender_id, created_at DESC")
})
```

#### 📊 성능 측정 결과

```
최대 지연 시간 (Max Latency)
[개선 전] ████████████████████████████████████████  753 ms
[개선 후] ██                                        30 ms  
```

| 측정 지표 | 개선 전 (Filesort) | 개선 후 (복합 인덱스) | 개선 성과 |
|-----------|-------------------:|---------------------:|-----------|
| 초당 처리량 (TPS) | 18.6 TPS | **99.7 TPS** |  5.3배 향상 |
| 최대 지연 (Max) | 753 ms | **30 ms** | 96% 단축 |
| 99% 응답 (p99) | 231 ms | **18 ms** | 92.2% 단축 |
| 95% 응답 (p95) | 112 ms | **15 ms** | 86.6% 단축 |
| 평균 응답 시간 | 34 ms | **12 ms** | 64.7% 단축 |

---

### 3. Facade 패턴을 통한 S3 I/O 격리 및 HikariCP 고갈 방어

> **대상 API:** `POST /api/v1/images/upload` (프로필 이미지 업로드)

#### 🚨 문제 정의

서비스 메서드 전체에 `@Transactional`이 걸려 있어, AWS S3 업로드(네트워크 I/O **~200ms**) 동안 DB 커넥션을 **독점 점유**하는 Connection Starvation 발생.  
동시 50명 스파이크 부하 시 커넥션 대기 큐로 인해 **지연 편차(Max - Min)가 198ms**까지 벌어짐.

#### 💡 해결: 트랜잭션 경계 분리 + 보상 트랜잭션

```
┌─────────────────────────────────────────────────────────────────┐
│  UserImageFacade (트랜잭션 없음)                                  │
│                                                                 │
│  1️⃣  S3 업로드 ──────────→ 완료 (DB 커넥션 점유 0ms)              │
│                                                                 │
│  2️⃣  try {                                                      │
│        userImageService.registerImage(...)  ← 짧은 트랜잭션만     │
│      } catch (e) {                                              │
│  3️⃣    fileUploader.delete(s3Path)  ← 보상 트랜잭션 (S3 롤백)     │
│        throw e                                                  │
│      }                                                          │
└─────────────────────────────────────────────────────────────────┘
```

```java
// UserImageFacade.java
String imageUrl = fileUploader.upload(imageFile, s3Path);  // 1. 트랜잭션 밖 (커넥션 점유 0ms)

try {
    userImageService.registerImageVerification(user, imageUrl, isMainImage);  // 2. 짧은 트랜잭션
} catch (Exception e) {
    fileUploader.delete(s3Path);  // 3. DB 실패 시 S3 즉시 롤백 (보상 트랜잭션)
    throw e;
}
```

#### 📊 성능 측정 결과

| 측정 지표 | 개선 전 (`@Transactional` 내 S3) | 개선 후 (Facade 분리) | 개선 성과 |
|-----------|--------------------------------:|---------------------:|-----------|
| 평균 응답 시간 | 311 ms | **237 ms** | 서버 오버헤드 67% 감소 |
| 최대 지연 (Max) | 428 ms | **259 ms** |  169ms 단축 |
| 95% 응답 (p95) | 402 ms | **250 ms** |  38% 안정화 |
| 지연 편차 (Max - Min) | 198 ms | **32 ms** |  균일성 83.8% 개선 |

---

### 4. 결제 중복 승인(따닥) 방어: 비관적 락 & 멱등성 이중 방어선

> **대상 API:** `POST /api/v1/payments/verify` (포인트 결제 승인)

#### 🚨 문제 정의

결제 승인 버튼 **더블 클릭(따닥)** 시 동일 주문에 대한 다중 요청 **Race Condition** 위험 존재.

#### 🔬 3단계 동시성 검증 실험 (JUnit 5, 10개 스레드 동시 인가)

| 실험 시나리오 | 성공 | 차단 | 최종 충전 | 결함 분석 |
|---------------|:----:|:----:|:---------:|-----------|
| **\[실험 1\]** 락 ✕, 검증 ✕ | 10건 | 0건 | 1,000P |  **갱신 분실**: 10개 스레드가 초기값 동시 읽기 후 덮어써서 9,000P 증발 |
| **\[실험 2\]** 락 ✓, 검증 ✕ | 10건 | 0건 | 10,000P |  **멱등성 결함**: 직렬화 성공했으나 상태 검증 부재로 10회 중복 충전 |
| **\[실험 3\]** 락 ✓, 검증 ✓ | **1건** | **9건** | **1,000P** |  **100% 무결성 방어**: 배타락 + PAID 상태 검증 |

#### 💡 최종 이중 방어선 코드

```java
// PaymentUpdateService.java
PaymentEntity payment = paymentRepository
    .findByMerchantUidWithLock(merchantUid)                    // 1. 배타락(SELECT ... FOR UPDATE)
    .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보 없음"));

if (payment.getStatus() == PaymentStatus.PAID) {               // 2. 멱등성 검증
    throw new InternalServerException(
        ErrorCode.INTERNAL_SERVER_ERROR, "이미 처리된 결제 건입니다.");
}

payment.completePayment(paymentId);                             // 3. 상태 전이: READY → PAID
pointService.chargePoints(                                      // 4. 포인트 1회 충전
    payment.getUser(), payment.getProduct().getPoint(),
    PointType.CHARGE, "포인트 충전");
```

---

## ⚖️ 기술적 트레이드오프 & 엔지니어링 회고

| 주제 | 트레이드오프 결정 |
|------|------------------|
| **JPA Fetch Join vs 페이징 OOM** | ToOne 관계는 카테시안 곱이 없어 `Fetch Join`으로 RTT 최소화, 컬렉션(ToMany)은 메모리 OOM 방지를 위해 `batch_fetch_size: 100`으로 분리하는 절충안 도출 |
| **분산 환경 정합성** | S3와 RDBMS는 단일 ACID 트랜잭션으로 묶을 수 없음 → Facade로 트랜잭션 분리 + DB 실패 시 S3 롤백 **보상 트랜잭션** 설계로 정합성 보완 |
| **비관적 락 vs 동시 처리량** | 1원의 오차도 불허하는 결제 특성상 Lock Wait를 감수하고 **비관적 배타락** 선택. 락 대상을 `merchant_uid`(UNIQUE) 인덱스로 국소화하여 타 유저 트랜잭션 격리 |

---

## 📁 프로젝트 구조

```
piqProject/
├── src/main/java/piq/piqproject/
│   ├── PiqProjectApplication.java
│   ├── common/                       # 공통 유틸, AOP, 글로벌 예외 처리
│   ├── config/                       # 설정 (Security, DB, S3, HikariCP)
│   ├── domain/                       # 도메인별 비즈니스 로직 (Layered Architecture)
│   │   ├── matches/                  #   [핵심] 매칭 라이프사이클 & 페이징 튜닝
│   │   ├── payments/                 #   [핵심] 결제 승인, 비관적 락 & 보상 트랜잭션
│   │   ├── userimages/               #   [핵심] 이미지 업로드 Facade & 보상 트랜잭션
│   │   ├── points/                   #   포인트 충전/차감 로직
│   │   ├── products/                 #   충전 상품 도메인
│   │   ├── users/                    #   사용자 도메인
│   │   └── ...
│   └── infra/                        # 외부 인프라 (AWS S3, PortOne API 클라이언트)
├── src/test/java/piq/piqproject/     # 동시성 및 단위/통합 테스트
│   └── PaymentConcurrencyTest.java   # 10개 스레드 결제 동시성 검증 테스트
├── docker-compose.yml                # PostgreSQL + Redis 실행 환경
└── build.gradle                      # 의존성 및 빌드 설정
```

---

## 🏁 시작하기

### 사전 요구 사항

- **Java 21** 이상
- **Docker & Docker Compose**
- **Git**

### 1. 저장소 클론

```bash
git clone https://github.com/PiqProject/piqProject.git
cd piqProject
```

### 2. Docker 컨테이너 실행 (PostgreSQL + Redis)

```bash
docker-compose up -d
```

### 3. 멀티스레드 동시성 테스트 실행 검증

결제 동시성 제어 로직(비관적 락 + 멱등성 이중 방어)이 정상 동작하는지 검증:

```bash
./gradlew test --tests piq.piqproject.PaymentConcurrencyTest
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

> 서버가 `http://localhost:8080`에서 시작됩니다.

---

## 🔐 환경 변수 가이드

`.env.example` 파일을 참고하여 `.env.properties`를 생성합니다:

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5433/piqdb` |
| `DB_USERNAME` | DB 사용자명 | `piq_user` |
| `DB_PASSWORD` | DB 비밀번호 | - |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `PORTONE_V2_SECRET` | PortOne V2 Secret Key | - |
| `S3_ENDPOINT` | AWS S3 / 스토리지 엔드포인트 | - |

> 전체 목록은 [`.env.example`](.env.example)을 참고하세요.

---

## 📄 라이선스

This project is licensed under the MIT License.
