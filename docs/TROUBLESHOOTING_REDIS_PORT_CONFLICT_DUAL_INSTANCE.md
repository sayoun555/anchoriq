# Redis 포트 충돌로 Docker Redis 대신 로컬 Redis 연결: DBSIZE 0 → 17,301건 정상 조회

## 배경 및 문제정의

AIS 선박 위치 데이터를 Redis GEO에 저장한 뒤 지도에 표시하는 파이프라인에서, `docker exec infra-redis-1 redis-cli DBSIZE`가 항상 0을 반환했다. Spring Boot 앱 내부 디버그 엔드포인트(`/api/debug/redis/dbsize`)로 확인하면 17,301건이 존재했다. 동일한 Redis를 바라보는데 결과가 다른 상황이었다.

원인은 **호스트에 Homebrew Redis와 Docker Redis가 동시에 6379 포트를 점유**하고 있었고, Spring Boot는 localhost:6379 → Homebrew Redis에, docker exec는 컨테이너 내부 Redis에 연결되어 서로 다른 인스턴스를 바라보고 있었다.

## 기술 선정 (대안 비교)

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| Homebrew Redis 중지 (`brew services stop redis`) | 포트 해제 간단 | 다른 프로젝트 영향, 재부팅 시 재시작 가능 | X |
| Docker Redis 포트를 6380으로 변경 | 충돌 완전 제거, 명시적 분리 | 설정 파일 변경 필요 | **O** |
| Spring Boot에서 Docker 내부 IP 직접 지정 | 포트 변경 불필요 | 컨테이너 IP 유동적, 유지보수 어려움 | X |

## 분석

### 초기 판단

"Redis에 데이터가 안 들어갔다"고 판단. Kafka Consumer 로그를 확인하니 정상 consume + Redis 저장 성공 로그가 찍히고 있었다.

### 실제 원인

```bash
# 포트 점유 확인
$ lsof -i :6379
COMMAND   PID  USER   FD   TYPE  DEVICE  NODE NAME
redis-ser 512  user   6u   IPv4  ...     TCP  *:6379 (LISTEN)   # Homebrew
com.docke 891  user   98u  IPv6  ...     TCP  *:6379 (LISTEN)   # Docker proxy
```

Homebrew Redis가 먼저 6379를 바인딩. Docker Desktop의 포트 매핑(`-p 6379:6379`)은 `SO_REUSEADDR`로 같은 포트에 바인딩을 시도하지만, macOS에서 IPv4/IPv6 스택이 분리되어 각각 LISTEN 상태가 된다. `localhost` 해석 시 IPv4(`127.0.0.1`)가 우선 → Homebrew Redis에 연결.

### CS 원리: TCP 포트 바인딩과 Docker Port Mapping

**TCP 포트 바인딩**: 커널은 `(protocol, local_addr, local_port)` 튜플로 소켓을 식별한다. `SO_REUSEADDR` 소켓 옵션이 설정되면 동일 포트에 여러 소켓이 바인딩 가능하되, 정확히 같은 주소(예: 둘 다 `0.0.0.0:6379`)면 충돌한다. 하지만 하나가 IPv4(`0.0.0.0`), 다른 하나가 IPv6(`::`)이면 커널이 별도 소켓으로 허용한다.

**Docker Port Mapping**: `docker-proxy` 프로세스가 호스트 포트에서 LISTEN → 수신된 패킷을 iptables/userland proxy로 컨테이너 veth 인터페이스에 전달. macOS에서는 LinuxKit VM 내부 포워딩이므로 호스트 네이티브 소켓과 충돌 감지가 불완전하다.

**Lettuce AUTH 동작**: Spring Data Redis의 Lettuce 클라이언트는 연결 시 `AUTH password` 명령을 전송한다. 비밀번호가 설정되지 않은 Redis(Homebrew)에 AUTH를 보내면 `ERR Client sent AUTH, but no password is set` 응답이 오지만, **Lettuce는 이를 무시하고 연결을 유지**한다. Redis AUTH 프로토콜(RESP)에서 AUTH 실패는 연결을 끊지 않으며, 이후 명령은 정상 실행된다.

```
# Redis AUTH 프로토콜 (RESP)
Client: *2\r\n$4\r\nAUTH\r\n$8\r\npassword\r\n
Server: -ERR Client sent AUTH, but no password is set\r\n  ← 에러지만 연결 유지
Client: *1\r\n$4\r\nPING\r\n
Server: +PONG\r\n  ← 정상 동작
```

## 솔루션

Docker Redis 포트를 6380으로 변경하여 충돌을 제거.

```yaml
# docker-compose.yml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6380:6379"        # 호스트 6380 → 컨테이너 6379
    command: redis-server --requirepass ${REDIS_PASSWORD}
```

```yaml
# application-local.yml
spring:
  data:
    redis:
      host: localhost
      port: 6380           # Docker Redis 전용 포트
      password: ${REDIS_PASSWORD}
```

```bash
# 검증
$ docker exec infra-redis-1 redis-cli -a $REDIS_PASSWORD DBSIZE
(integer) 17301

$ redis-cli -p 6380 -a $REDIS_PASSWORD DBSIZE
(integer) 17301   # 동일 인스턴스 확인
```

## 결과 (Before / After)

| 지표 | Before | After |
|------|--------|-------|
| `docker exec redis-cli DBSIZE` | 0 (잘못된 인스턴스) | 17,301 |
| Spring Boot 연결 대상 | Homebrew Redis (6379/IPv4) | Docker Redis (6380) |
| AIS 선박 지도 표시 | 0척 | 17,000+척 |
| 포트 충돌 | IPv4/IPv6 이중 LISTEN | 완전 분리 |
| 디버깅 소요 시간 | 2시간 (원인 불명) | 즉시 확인 가능 |
