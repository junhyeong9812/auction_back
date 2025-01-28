# Auction(Backend) - README

## 목차
1. [프로젝트 개요](#1-프로젝트-개요)  
2. [Gradle 설정](#2-gradle-설정)  
3. [애플리케이션 설정(`application.yml`)](#3-애플리케이션-설정applicationyml)  
4. [Docker / Nginx / Certbot 설정](#4-docker--nginx--certbot-설정)  
   - [4.1 docker-compose.yml](#41-docker-composeyml)  
   - [4.2 Redis Dockerfile](#42-redis-dockerfile)  
   - [4.3 Nginx 설정](#43-nginx-설정)  
   - [4.4 Certbot (Let’s Encrypt)](#44-certbot-lets-encrypt)  
5. [프로젝트 구조](#5-프로젝트-구조)  
6. [주요 도메인 / 기능](#6-주요-도메인--기능)  
   - [6.1 사용자(User) 도메인](#61-사용자user-도메인)  
   - [6.2 경매(Auction) 도메인](#62-경매auction-도메인)  
   - [6.3 결제(Payment) 도메인](#63-결제payment-도메인)  
   - [6.4 WebSocket (경매 실시간 입찰)](#64-websocket-경매-실시간-입차료)  
7. [구동 방법](#7-관당-방법)  
   - [7.1 로컬 개발 환경](#71-로케어-개발-환경)  
   - [7.2 Docker Compose 전체 실행(예시)](#72-docker-compose-전체-실행예시)  
   - [7.3 HTTPS/SSL (Certbot)](#73-httpsssl-certbot)  
8. [기타 참고사항](#8-기타-참고사항)  
9. [주요 API 요약](#9-주요-api-요약)  
10. [마무리](#10-마무리)  

---

## 1. 프로젝트 개요

- **프로젝트 요약**  
  반려동물 경매 시스템을 예시로 한 프로젝트로,
  - 경매 등록, 수정, 취소
  - 실시간 WebSocket(SockJS + STOMP) 을 통한 입찰
  - Redis를 이용한 캐싱 및 실시간 경매 데이터 관리
  - Spring Security + JWT 이용한 인증/인가
  - PortOne(아임포트) 결제 연동
  - Docker + Nginx + Certbot을 통한 HTTPS/SSL 구성
  등을 포함합니다.

- **주요 기술스택**  
  - Java 17
  - Spring Boot 3.x
  - Spring Data JPA, Spring Data Redis
  - Spring Security, JWT
  - WebSocket(STOMP, SockJS)
  - QueryDSL
  - H2 (인메모리 DB, 개발/테스트용)
  - Docker, Docker Compose
  - Nginx, Certbot (Let’s Encrypt)
  - Redis
  - Lombok 등

---

## 2. Gradle 설정

`build.gradle` 내 주요 플러그인/의존성은 아래와 같습니다.

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.auction'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web (MVC, REST)
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // JPA/Hibernate (RDB 연동)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // Redis 연동
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // WebSocket (실시간 통신)
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // 인메모리 DB (테스트/개발용)
    runtimeOnly 'com.h2database:h2'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    // p6spy (SQL 로그)
    implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.0'

    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
    annotationProcessor "jakarta.annotation:jakarta.annotation-api"
    annotationProcessor "jakarta.persistence:jakarta.persistence-api"

    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // JWT (io.jsonwebtoken: JJWT)
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'

    // 테스트 라이브러리
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## 3. 애플리케이션 설정(`application.yml`)

개발/테스트 환경에서 사용하는 기본 설정 예시는 다음과 같습니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true
  servlet:
    multipart:
      max-file-size: 1GB
      max-request-size: 1GB
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 2000
        one-indexed-parameters: true
  redis:
    host: localhost
    port: 6379

logging:
  level:
    org.hibernate.SQL: debug

jwt:
  secret:  # JWT 시크릿키
  access-expiration: 1800000     # 30분(ms)
  refresh-expiration: 86400000   # 1일(ms)

imp:
  key:
  secret:
  pg:
  storeId:
```
- 실제 운영환경에서는 `db` 연결 정보, `jwt.secret`, `imp.key`/`imp.secret` 등 민감정보를 별도 환경변수나 `application-prod.yml` 등에 분리하여 관리합니다.
- Redis나 DB 호스트/포트도 docker-compose 환경에 따라 달라질 수 있습니다.

---

## 4. Docker / Nginx / Certbot 설정

### 4.1 docker-compose.yml

아래 예시는 `docker/dev/docker-compose.yml` 에서 사용하는 예시입니다.  
(경로는 프로젝트 설정에 따라 다를 수 있습니다.)

```yaml
version: '3'
services:
#  app:
#    build:
#      context: ../..
#      dockerfile: docker/app/Dockerfile.app
#    container_name: my-auction-app
#    ports:
#      - "8080:8080"
#    depends_on:
#      - redis
#    environment:
#      SPRING_REDIS_HOST: "redis"
#      SPRING_REDIS_PORT: "6379"

  redis:
    build:
      context: ../..
      dockerfile: docker/redis/Dockerfile-redis
    container_name: my-custom-redis
    ports:
      - "6379:6379"

  nginx:
    image: nginx:alpine
    container_name: local-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ../nginx/default.conf:/etc/nginx/conf.d/default.conf:ro
      - ../certbot/www:/var/www/certbot
      - ../certbot/letsencrypt:/etc/letsencrypt
    depends_on:
      - certbot
    restart: unless-stopped

  certbot:
    image: certbot/certbot
    container_name: local-certbot
    volumes:
      - ../certbot/www:/var/www/certbot
      - ../certbot/letsencrypt:/etc/letsencrypt
    entrypoint: /bin/sh
    # 인증서 발급 시:
    # docker-compose run --rm certbot certbot certonly ...
```

#### 4.1.1 `app` 컨테이너  
원한다면, 위 예시에서 주석 처리된 `app` 서비스 부분을 해제하여 사용합니다.  
```yaml
  app:
    build:
      context: ../..
      dockerfile: docker/app/Dockerfile.app
    container_name: my-auction-app
    ports:
      - "8080:8080"
    depends_on:
      - redis
    environment:
      SPRING_REDIS_HOST: "redis"
      SPRING_REDIS_PORT: "6379"
      # DB, JWT Secret 등 필요한 환경변수 추가
```

### 4.2 Redis Dockerfile
`docker/redis/Dockerfile-redis` 예시:

```dockerfile
FROM redis:7.2

# 필요한 경우 redis.conf 복사/적용
# COPY redis.conf /usr/local/etc/redis/redis.conf

CMD ["redis-server"]
```

### 4.3 Nginx 설정

`nginx/default.conf` 예시:

```nginx
# 1) HTTP server
server {
    listen 80;
    server_name www.pinjun.xyz;

    # certbot 인증용(HTTP-01)
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # HTTP -> HTTPS 리다이렉트
    location / {
        return 301 https://$host$request_uri;
    }

    # (옵션) 나머지 모든 경로는 React, Spring 등에 프록시
    # location /api/ {
    #     proxy_pass http://host.docker.internal:8080;
    # }
    # location / {
    #     proxy_pass http://host.docker.internal:3000;
    # }
}

# 2) HTTPS server
server {
    listen 443 ssl;
    server_name www.pinjun.xyz;

    ssl_certificate     /etc/letsencrypt/live/www.pinjun.xyz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/www.pinjun.xyz/privkey.pem;

    # (1) /api => 호스트(PC)의 Spring(8080) 프록시
    location /api/ {
        proxy_pass http://host.docker.internal:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # (2) /ws-stomp => 호스트(PC)의 Spring(8080) 웹소켓
    location /ws-stomp {
        proxy_pass http://host.docker.internal:8080/ws-stomp;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # (3) 기타 => 호스트(PC)의 React Dev Server (3000)
    location / {
        proxy_pass http://host.docker.internal:3000/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade; # for hot reload websockets
        proxy_set_header Connection "upgrade";
    }
}
```

> **주의**  
> - `host.docker.internal` 은 **Windows/Mac** Docker Desktop 환경에서만 동작합니다.  
> - **Linux** 환경에선 실제 호스트 IP를 직접 기입해야 합니다(예: `192.168.0.10`).  
> - `server_name www.pinjun.xyz` 는 실제 발급받은 도메인으로 변경하시면 됩니다.

### 4.4 Certbot (Let’s Encrypt)

1. **DNS 설정**  
   - Cloudflare, 가비아, AWS Route53 등에서 A레코드를 등록하여,  
     `yourdomain.com` → (서버IP) 연결
2. **Docker Compose 컨테이너 실행**  
   - 먼저 `nginx`, `certbot` 컨테이너를 띄워둡니다:
     ```bash
     docker-compose up -d nginx certbot
     ```
   - 80 포트가 열려 있어야 Let’s Encrypt 인증이 가능합니다.
3. **인증서 발급 예시 명령어**  
   - 아래에서 `youremail@domain.com`, `yourdomain.com` 부분을 실제 값으로 바꿔 입력합니다.
     ```bash
     docker-compose run --rm certbot \
       certbot certonly --webroot \
       -w /var/www/certbot \
       -d "yourdomain.com" \
       -d "www.yourdomain.com" \
       --email youremail@domain.com \
       --agree-tos \
       --no-eff-email
     ```
   - 만약 서브도메인이 여러 개면 `-d` 파라미터를 추가로 붙일 수 있습니다.
4. **Nginx SSL 경로 설정**  
   - 인증서가 성공적으로 발급되면 `/etc/letsencrypt/live/yourdomain.com/` 아래 `.pem` 파일들이 생성됩니다.
   - `nginx/default.conf` 내 `ssl_certificate`, `ssl_certificate_key` 경로를 위 경로와 일치시키면 HTTPS가 활성화됩니다.

---

## 5. 프로젝트 구조

아래와 같은 패키지 구조를 사용하고 있습니다.

```
└─src
   ├─main
   │  ├─java
   │  │  └─com
   │  │      └─auction
   │  │          └─back
   │  │              ├─domain
   │  │              │  ├─auction
   │  │              │  │  ├─controller
   │  │              │  │  │  ├─command
   │  │              │  │  │  ├─image
   │  │              │  │  │  ├─query
   │  │              │  │  │  └─ws
   │  │              │  │  ├─dto
   │  │              │  │  │  ├─request
   │  │              │  │  │  ├─response
   │  │              │  │  │  └─ws
   │  │              │  │  ├─entity
   │  │              │  │  ├─enums
   │  │              │  │  ├─repository
   │  │              │  │  ├─scheduler
   │  │              │  │  └─service
   │  │              │  │      ├─command
   │  │              │  │      ├─query
   │  │              │  │      ├─scheduled
   │  │              │  │      └─ws
   │  │              │  ├─payment
   │  │              │  │  ├─controller
   │  │              │  │  │  ├─command
   │  │              │  │  │  └─query
   │  │              │  │  ├─dto
   │  │              │  │  │  ├─request
   │  │              │  │  │  └─response
   │  │              │  │  ├─entity
   │  │              │  │  ├─enums
   │  │              │  │  ├─infrastructure
   │  │              │  │  ├─repository
   │  │              │  │  └─service
   │  │              │  │      ├─command
   │  │              │  │      └─query
   │  │              │  └─user
   │  │              │      ├─controller
   │  │              │      │  ├─auth
   │  │              │      │  ├─command
   │  │              │      │  └─query
   │  │              │      ├─dto
   │  │              │      │  ├─request
   │  │              │      │  └─response
   │  │              │      ├─entity
   │  │              │      ├─enums
   │  │              │      ├─repository
   │  │              │      └─service
   │  │              │          ├─auth
   │  │              │          ├─command
   │  │              │          ├─details
   │  │              │          └─query
   │  │              └─global
   │  │                  ├─config
   │  │                  │  └─websocket
   │  │                  ├─entity
   │  │                  ├─enums
   │  │                  ├─jwt
   │  │                  ├─redis
   │  │                  └─utils
   │  └─resources
   │      ├─images
   │      │  └─auction
   │      ├─static
   │      └─templates
   └─test
       └─java
           └─com
               └─auction
                   └─back
```

### 5.1 주요 패키지 설명

- **domain**  
  - `auction`: 경매 기능 관련(엔티티, 컨트롤러, 서비스, 스케줄러, WebSocket 등)  
  - `payment`: 결제 기능 관련(결제 엔티티, Controller, PortOne 연동 등)  
  - `user`: 사용자 관련(가입, 로그인, JWT 토큰발급, 정보조회 등)
- **global**  
  - 전역 설정(`config`), 인증/보안(JWT, Security), Redis 유틸, 공통 유틸 등
- **resources**  
  - `images/auction`: 경매 이미지 저장
  - `static`, `templates`: (필요 시) 정적 리소스, 템플릿 등

---

## 6. 주요 도메인 / 기능

### 6.1 사용자(User) 도메인

- **엔티티**: `User`  
  - 이메일, 비밀번호, 닉네임, 포인트 잔액, 역할/상태 등 관리
- **Controller**  
  - `UserCommandController` (회원가입)  
  - `UserQueryController` (유저 목록 등 조회)
  - `UserAuthController` (로그인, 토큰 재발급 등)
- **JWT 기반 로그인**  
  - AccessToken/RefreshToken을 **쿠키**로 발급  
  - RefreshToken으로 재발급 시 `/api/auth/refresh` 사용
- **포인트**  
  - 결제 시 유저 포인트 충전/차감

### 6.2 경매(Auction) 도메인

- **엔티티**: `Auction`  
  - 제목, 시작가, 시작/마감 시간, 이미지, 상태(SCHEDULED/ONGOING/ENDED/CANCELED), 낙찰자, 낙찰가 등  
- **컨트롤러**  
  - `AuctionCommandController` (등록, 취소, 수정)  
  - `AuctionQueryController` (검색, 상세 조회)  
  - `AuctionImageController` (이미지 로드)  
  - `AuctionStompController` (WebSocket 실시간 입찰)
- **스케줄러**  
  - `AuctionScheduler` (또는 `AuctionScheduledService` 내부)  
  - 매 1초마다 **SCHEDULED** → **ONGOING** 전환, **ONGOING** → **ENDED** 마감 처리
- **Redis**  
  - 실시간 입찰 시 최고가, 최고입찰자, 동적 마감시간 등을 Redis에 보관  
  - 마감 시 Redis 데이터(최고 입찰정보) → DB에 반영

### 6.3 결제(Payment) 도메인

- **엔티티**: `Payment`  
  - PortOne(아임포트)에서 받은 `impUid`(결제건의 고유 ID), 결제금액, 결제수단/상태(PAID/CANCELED/FAILED), 구매자 정보 등
- **PortOne(아임포트) 연동**  
  - 결제 완료 후 **impUid**로 결제검증 → DB 저장  
  - 결제 상태가 PAID면 사용자 포인트 충전  
- **Controller**  
  - `PaymentCommandController` (결제 완료 후 `POST /api/payment/complete` 호출)
  - 추후 결제 이력 조회 API 등은 `PaymentQueryController`로 확장 가능

### 6.4 WebSocket (경매 실시간 입찰)

- **엔드포인트**: `"/ws-stomp"`  
- **설정**  
  - `WebSocketConfig`에서 `@EnableWebSocketMessageBroker` 사용  
  - `/topic` → Broadcast용, `/user` → 1:1 개인채널, `/app` → 클라이언트→서버 메시지 전송 prefix
- **StompController**  
  - `@MessageMapping("/auction/{auctionId}/bid")`  
  - 클라이언트가 `stompClient.send("/app/auction/" + auctionId + "/bid", ...)`로 요청 시 처리
- **JWT 핸드셰이크**  
  - `JwtHandshakeInterceptor`, `CustomHandshakeHandler`를 통해 WebSocket Connection handshake 시 **쿠키의 JWT 토큰** 검증 및 `Principal` 설정
- **실시간 연장 로직**  
  - 마감 3분 이내 입찰 발생 시, 마감시간 5분 연장
  - Redis에 `endTime` 업데이트 → 스케줄러가 마감 시점 체크

---

## 7. 구동 방법

### 7.1 로컬 개발 환경

1. **Redis(로컬 or Docker)**  
   - 로컬 PC에 Redis를 설치하거나, `docker-compose up redis` 등을 통해 실행  
   - `application.yml`의 `spring.redis.host` / `port`를 맞춰준다.
2. **H2 인메모리 DB**  
   - 별도 설치 불필요, 자동 실행
3. **Gradle**  
   - `./gradlew bootRun` (또는 IntelliJ 등 IDE에서 실행)  
   - 기본적으로 `http://localhost:8080` 에서 동작
4. **React(Front)**가 있을 경우 3000 포트로 실행, CORS 혹은 Proxy 설정이 필요

### 7.2 Docker Compose 전체 실행(예시)

1. `docker/dev/docker-compose.yml` 에서 `app` 주석 해제 후 원하는 Dockerfile 설정
2. 루트 폴더(또는 `docker/dev`)에서  
   ```bash
   docker-compose up --build
   ```
3. Nginx, Redis, Certbot, App 등이 함께 구동됩니다.  
   - Nginx : 80, 443 포트  
   - Redis : 6379 포트  
   - App : 8080 포트 (호스트 기준)

### 7.3 HTTPS/SSL (Certbot)

1. DNS 설정 (예: `yourdomain.com` → 서버 Public IP)
2. `docker-compose up -d nginx certbot` 실행
3. **인증서 발급 명령어 예시**:
   ```bash
   docker-compose run --rm certbot \
     certbot certonly --webroot \
     -w /var/www/certbot \
     -d "yourdomain.com" \
     -d "www.yourdomain.com" \
     --email youremail@domain.com \
     --agree-tos \
     --no-eff-email
   ```
4. Nginx 설정에서 `ssl_certificate`, `ssl_certificate_key` 경로를 인증서 발급 경로에 맞게 설정

---

## 8. 기타 참고사항

1. **JWT 쿠키 설정**  
   - `UserAuthController` 에서 로그인 시 `Set-Cookie: ACCESS_TOKEN, REFRESH_TOKEN` 헤더로 반환  
   - `HttpOnly`, `Secure(true)`, `SameSite=None` 등 설정
2. **WebSocket 쿠키**  
   - SockJS STOMP 연결 시 자동으로 쿠키를 포함  
   - `JwtHandshakeInterceptor`에서 쿠키 내 `ACCESS_TOKEN`을 검증
3. **DB 교체**  
   - 운영 시 H2 → MySQL/PostgreSQL 등의 실제 RDB로 교체 가능 (`spring.datasource.url` 수정)
4. **호스트 IP 변경** (Linux 환경)  
   - Nginx 프록시 설정에서 `proxy_pass http://host.docker.internal:8080;` 부분을 실제 호스트 IP로 교체 필요
5. **이미지 업로드**  
   - 경매 이미지(멀티파트파일) 업로드 시 `src/main/resources/images/auction` 폴더에 저장 (개발용)
   - 운영 시에는 AWS S3, NFS 등의 외부 스토리지를 사용하는 것이 일반적

---

## 9. 주요 API 요약

- **[User]**  
  - `POST /api/users/register` : 회원가입  
  - `POST /api/auth/login` : 로그인 → 쿠키에 Access/Refresh Token  
  - `POST /api/auth/refresh` : 리프레시 토큰으로 Access 토큰 재발급  
  - `GET /api/users` : 사용자 목록 조회  

- **[Auction]**  
  - `POST /api/auction` : 경매 생성  
  - `POST /api/auction/{id}/cancel` : 경매 취소  
  - `PATCH /api/auction/{id}` : 경매 수정  
  - `GET /api/auctions/search` : 경매 목록 검색 + 페이지네이션  
  - `GET /api/auctions/{auctionId}` : 경매 상세 조회  
  - **WebSocket**: `"/app/auction/{auctionId}/bid"` → 입찰  

- **[Payment]**  
  - `POST /api/payment/complete` : 결제 완료시 PortOne 검증 후 DB 저장/포인트 반영

---

## 10. 마무리

이상으로 **Auction** 프로젝트의 백엔드(Spring Boot) 구성을 간단히 정리했습니다.  
추가로 로컬 및 서버 환경에 맞게 설정(YML), Dockerfile, Nginx, Certbot 등을 수정/보완하여 사용하시면 됩니다.

> **문의/이슈**  
> - 새 기능 추가나 버그 발견 시 이슈 트래커를 통해 공유 부탁드립니다.  
> - CI/CD, 고가용성 설계, 추가 보안(Security) 설정 등은 프로젝트 요구사항에 따라 확장 가능합니다.

감사합니다.





