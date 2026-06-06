# 알림이 실제로 발송되지 않던 문제 — 목적지(destination) 미영속

> 하네스 검증단(다수결 패널)이 표면화한 실제 기능 버그. 하네스 프로세스(설계먼저 → 단일 구현 → ground-check 컴파일 → check-stubs 게이트)로 수정.

## 문제

리스크 알림 규칙이 매칭돼도 **Slack/이메일이 실제로 도착하지 않는다.** 로그상 발송 호출은 일어나지만, SlackNotifier가 받은 destination이 웹훅 URL이 아니라 `"HIGH"`(매칭 조건값)였다.

```
dispatch(event) → 매칭 규칙 → sendAndRecord(rule)
   → resolveDestination(rule) == rule.getConditionValue()  // "HIGH"
   → SlackNotifier.send("HIGH", ...) → WebClient.post().uri("HIGH") → 발송 실패
```

## 원인 — 단순 버그가 아니라 설계 갭

`NotificationDispatcherImpl.resolveDestination()`이 `conditionValue`를 destination으로 반환하고 있었고(코드에 `// 임시로...` 주석까지 있었음), 그 근본은:

1. `notification_rules` 스키마에 **발송 대상(destination) 컬럼이 없음** — condition_type/value(언제) + channel(SLACK/EMAIL)만.
2. `NotificationSettingsRequest/Response` DTO엔 `slackWebhookUrl`·`emailAddress`가 있지만 — **어디에도 영속되지 않음.** `getSettings()`는 enabled 여부·규칙 수만 반환했고, `updateSettings`는 컨트롤러에서 body도 안 받는 stub이었다.
3. 즉 **유저의 실제 목적지(웹훅 URL/이메일)를 저장할 곳이 시스템에 없었다.** dispatcher는 채울 게 없어 conditionValue를 끌어다 쓴 것.

## 해결 — 유저별 알림 설정(NotificationSettings) 신설

"규칙 = 언제/어느 채널", "설정 = 어디로 보낼지"로 **관심사를 분리**했다.

1. **스키마**: `notification_settings` 테이블 신설 (user_id UNIQUE, slack_enabled, slack_webhook_url, email_enabled, email_address). `infra/sql/init.sql` + `plan/DB_INIT_SCRIPTS.md` 동기화.
2. **도메인**(core): `NotificationSettings` 엔티티 — 목적지 결정을 도메인이 책임(Tell-Don't-Ask): `destinationFor(channel)`, `hasDestinationFor(channel)`. + `NotificationSettingsRepository` 인터페이스.
3. **영속**(api): `JpaNotificationSettingsRepository` + `NotificationSettingsRepositoryImpl`.
4. **응용**(api): `updateSettings(userId, request)` 업서트 + `getSettings`가 저장된 목적지를 반환. 컨트롤러 `PUT /settings`가 body를 받아 실제 저장.
5. **디스패처**(automation): `resolveDestination(rule)`이 규칙 소유자의 설정에서 채널별 목적지를 조회. **목적지가 없으면 발송 생략**(가짜 값으로 호출하지 않음).
   ```java
   private String resolveDestination(NotificationRule rule) {
       return settingsRepository.findByUserId(rule.getUserId())
               .filter(s -> s.hasDestinationFor(rule.getChannel()))
               .map(s -> s.destinationFor(rule.getChannel()))
               .orElse(null);
   }
   ```
6. **배선**(AutomationConfig): dispatcher 빈에 `NotificationSettingsRepository` 주입.

## 하네스로 어떻게 잡았나 (dogfooding)

- **발견**: 채널 B 다수결 검증단(`verify-findings.workflow.js`)이 code-reality 렌즈로 "conditionValue→destination 실제 발송 실패 가능" 소수의견을 냄.
- **설계먼저**: `plan-search.sh`로 `notification_rules` 스키마 절만 추출 → "목적지 컬럼 없음"을 확인(통째 읽기 X, 중간소실 회피).
- **임시방편 금지**: 한 줄 패치 대신 설계 갭(목적지 저장소)을 메우는 정석으로.
- **단일 에이전트**: 엔티티→리포→서비스→디스패처가 의존 사슬이라 단일로 일관되게(멀티 X).
- **외부 grounding**: 각 모듈 수정 후 `ground-check.sh`로 실제 컴파일(core/api/automation 모두 ✅).
- **게이트**: `check-stubs.sh`로 `// 임시` 마커 제거 확인(1건→0).

## CS 원리

- **관심사 분리(SoC)**: 트리거 조건(rule)과 전달 설정(settings)을 분리. 한 테이블에 섞으면 규칙마다 목적지 중복·정합성 깨짐. 설정을 유저당 1행으로 정규화.
- **Tell-Don't-Ask / 풍부한 도메인**: "이 채널의 목적지가 뭐냐"를 호출부가 분기하지 않고 `settings.destinationFor(channel)`로 도메인이 결정. 비활성/미설정이면 null → 발송부는 단순.
- **Fail-safe**: 목적지 미설정 시 가짜 값으로 외부 호출하지 않고 생략 + warn 로그. 잘못된 부수효과(엉뚱한 POST)를 원천 차단.
- **외부 grounding > 자기판단**: 수정이 맞는지 LLM 추측이 아니라 실제 컴파일로 검증(하네스 ground-check).
