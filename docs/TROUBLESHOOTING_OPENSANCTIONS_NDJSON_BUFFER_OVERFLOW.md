# OpenSanctions 수집기 SUCCESS + 0건: URL 오류 + DataBufferLimitException → 2,002건 수집

## 배경 및 문제정의

OpenSanctions 제재 데이터 수집기가 `CollectorStatus.SUCCESS`를 반환하면서 Kafka로 0건의 제재 데이터를 발송했다. 두 단계에 걸쳐 문제가 발견되었다.

1단계: API URL(`index.json`)이 메타데이터만 반환하여 entities 배열이 없었음.
2단계: 올바른 URL(`entities.ftm.json`)로 변경 후 `DataBufferLimitException`이 발생하여 응답 수신 자체가 실패.

## 기술 선정 (대안 비교)

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| RestTemplate + InputStream 스트리밍 | 메모리 효율적 | 블로킹 I/O, Reactive 파이프라인과 불일치 | X |
| WebClient + 버퍼 크기 증가 (10MB) | 간단한 설정 변경 | 대용량 시 메모리 부담 | **O** |
| WebClient + Flux 스트리밍 (line-by-line) | 메모리 최적, 백프레셔 | NDJSON 파싱 복잡도 증가 | 향후 개선 |
| 파일 다운로드 후 로컬 파싱 | 네트워크 재시도 용이 | 디스크 I/O 추가, 임시파일 관리 | X |

## 분석

### 초기 판단 (1단계 - 잘못된 URL)

OpenSanctions API 문서에서 `https://data.opensanctions.org/datasets/latest/default/index.json`을 엔드포인트로 사용. 응답은 데이터셋 메타데이터(이름, 설명, 통계)만 포함. `entities` 키가 존재하지 않으므로 빈 리스트로 처리 → 0건.

```json
// index.json 응답 (메타데이터)
{
  "name": "default",
  "title": "OpenSanctions Default",
  "resources": [
    { "name": "entities.ftm.json", "url": "https://data.opensanctions.org/.../entities.ftm.json" }
  ]
}
```

실제 데이터는 `resources[].url`에 명시된 `entities.ftm.json` (NDJSON 형식, 약 180MB).

### 실제 원인 (2단계 - 버퍼 초과)

URL 수정 후 WebClient로 요청하면 Spring WebFlux의 기본 버퍼 제한(256KB)을 초과하여 `DataBufferLimitException: Exceeded limit on max bytes to buffer: 262144` 발생.

### CS 원리: NDJSON, Reactive Streams, DataBuffer 메모리 관리

**NDJSON vs JSON Array**: 일반 JSON Array(`[{...}, {...}]`)는 전체를 파싱해야 구조를 알 수 있다. NDJSON(Newline-Delimited JSON)은 각 줄이 독립적인 JSON 객체로, 줄 단위 스트리밍 파싱이 가능하다.

```
// JSON Array - 전체 버퍼링 필수
[{"id":"Q123","schema":"Person","properties":{"name":["Kim"]}},
 {"id":"Q456","schema":"Person","properties":{"name":["Lee"]}}]

// NDJSON - 줄 단위 파싱 가능
{"id":"Q123","schema":"Person","properties":{"name":["Kim"]}}
{"id":"Q456","schema":"Person","properties":{"name":["Lee"]}}
```

**Reactive Streams Backpressure**: Reactor의 `Flux`는 Publisher-Subscriber 모델에서 Subscriber가 처리 가능한 만큼만 요청하는 **backpressure** 메커니즘을 제공한다. `WebClient.retrieve().bodyToMono(String.class)`는 전체 응답을 하나의 `String`으로 버퍼링하므로 backpressure의 이점을 상실한다.

**Spring WebFlux DataBuffer 메모리 관리**: Netty 기반 WebClient는 `ByteBuf` 풀(PooledByteBufAllocator)을 사용하여 Direct Memory에서 버퍼를 할당한다. `DataBuffer`는 이 `ByteBuf`의 래퍼로, reference counting으로 메모리를 관리한다.

```
HTTP 응답 수신 흐름:
Network → Netty Channel → ByteBuf (Direct Memory) → DataBuffer → Decoder

기본 제한: maxInMemorySize = 256KB (256 * 1024 = 262,144 bytes)
이 제한은 Decoder(Jackson, StringDecoder 등)가 전체 응답을 메모리에 축적할 때 적용.
```

제한이 존재하는 이유는 **메모리 고갈 방지**다. 악의적이거나 예상치 못한 대용량 응답이 JVM Direct Memory를 소진하면 `OutOfDirectMemoryError`가 발생하여 전체 애플리케이션이 중단된다.

**ExchangeStrategies codec 설정**: `ExchangeStrategies`는 WebClient의 인코더/디코더 설정을 관리한다. `maxInMemorySize`를 변경하면 내부적으로 `Jackson2JsonDecoder`, `StringDecoder` 등 모든 codec의 버퍼 제한이 조정된다.

**스트리밍 vs 버퍼링 트레이드오프**: 버퍼링(`bodyToMono`)은 구현이 간단하지만 메모리 사용량이 응답 크기에 비례. 스트리밍(`bodyToFlux` + line split)은 일정한 메모리로 무한 크기 응답 처리 가능하지만 에러 처리와 재시도 로직이 복잡해진다.

## 솔루션

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient sanctionsWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))  // 10MB
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .build();
    }
}
```

```java
@RequiredArgsConstructor
public class OpenSanctionsCollector implements SanctionDataCollector {

    private final WebClient sanctionsWebClient;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, SanctionEvent> kafkaTemplate;

    private static final String ENTITIES_URL =
        "https://data.opensanctions.org/datasets/latest/default/entities.ftm.json";

    @Override
    public CollectorResult collect() {
        String ndjsonBody = sanctionsWebClient.get()
            .uri(ENTITIES_URL)
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofMinutes(3));

        if (ndjsonBody == null || ndjsonBody.isBlank()) {
            return CollectorResult.failure("Empty response from OpenSanctions");
        }

        // NDJSON 줄 단위 파싱
        List<SanctionEntity> entities = ndjsonBody.lines()
            .filter(line -> !line.isBlank())
            .map(this::parseEntity)
            .filter(Objects::nonNull)
            .filter(entity -> "LegalEntity".equals(entity.getSchema())
                           || "Person".equals(entity.getSchema()))
            .toList();

        entities.forEach(entity ->
            kafkaTemplate.send("sanctions-data",
                entity.getId(), SanctionEvent.from(entity)));

        return CollectorResult.success(entities.size());
    }

    private SanctionEntity parseEntity(String line) {
        try {
            return objectMapper.readValue(line, SanctionEntity.class);
        } catch (JsonProcessingException e) {
            log.debug("NDJSON 라인 파싱 스킵: {}", e.getMessage());
            return null;
        }
    }
}
```

## 결과 (Before / After)

| 지표 | Before (1단계) | Before (2단계) | After |
|------|---------------|---------------|-------|
| API URL | index.json (메타데이터) | entities.ftm.json | entities.ftm.json |
| WebClient 버퍼 | 256KB (기본값) | 256KB (기본값) | 10MB |
| 에러 | 없음 (빈 응답 정상 처리) | DataBufferLimitException | 없음 |
| CollectorStatus | SUCCESS | FAILURE | SUCCESS |
| 제재 데이터 수집 | 0건 | 0건 | 2,002건 |
| 파싱 형식 | JSON Array 시도 | N/A (수신 실패) | NDJSON line-by-line |
