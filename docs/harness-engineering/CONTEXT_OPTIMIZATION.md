# 컨텍스트 최적화 — 긴 설계문서의 "중간 소실" 방지

## 문제

`plan/`에는 21개 설계문서가 있고 일부는 길다(예: `ARCHITECTURE.md` 373줄). 이걸 **통째로 컨텍스트에 올리면** 두 가지가 터진다:

1. **Lost in the Middle** — 트랜스포머는 컨텍스트 시작·끝은 또렷이, **중간은 약하게** 본다(U자 recall). 긴 문서를 다 넣으면 가운데 규칙이 묻힌다.
2. **Context Rot** — 창(1M 토큰) 안에 들어가더라도, 토큰이 많아질수록 **개별 정보의 신뢰도가 떨어진다.** 작은 컨텍스트가 큰 컨텍스트보다 정확하다.

즉 "설계문서를 많이 넣을수록 잘 따른다"는 직관은 틀렸다. **관련된 것만, 작게** 넣어야 한다.

## 원칙

| 원칙 | 의미 |
|------|------|
| 덤프하지 말고 인덱스+검색 | 21개 문서를 다 넣지 말고, 목록만 + 필요 시 fetch |
| 문서가 아니라 절(section) | 373줄 문서 대신 관련 13줄 절만 |
| 위치를 활용 | 핵심은 컨텍스트 시작·끝(주목도 강한 위치)에 |
| 압축 대비 영속화 | 핵심 사실은 파일/메모리에 적재해 재주입 |

## 구현 — `plan-search.sh`

설계문서를 통째로 `Read` 하는 대신 세 가지로 가져온다:

```bash
scripts/harness/plan-search.sh toc <문서>                  # 목차(헤더)만 — 어떤 절이 있나
scripts/harness/plan-search.sh section <문서> "<키워드>"   # 그 절만 (하위 절 포함)
scripts/harness/plan-search.sh grep "<질의>"               # plan/ 전체에서 관련 라인
```

**효과**(실측):
```
plan/ARCHITECTURE.md 전체          : 373줄
  └ section "Domain Service"       :  13줄   ← 96% 절감
  └ section "Aggregate Root"       :  15줄
```

`section`은 BSD awk로 헤더 레벨을 계산해, 키워드 헤더부터 **같은/상위 레벨 헤더 직전까지**(=하위 절 포함, 형제 절 제외) 추출한다.

## 하네스 배선 (두 지점)

1. **PreToolUse `governing-doc.sh`** — 코드 쓰기 직전, 지배 문서를 `§키워드`까지 찍고 **"통째로 읽지 말고 `plan-search.sh section`으로 그 절만"** 이라고 주입.
   ```
   📐 …지배 문서 → plan/ARCHITECTURE.md(§Domain Service)…
      통째로 읽지 말고 §표시된 절만: plan-search.sh section <문서> '<§키워드>'
   ```
2. **PostToolUse 의미 적대자 `review-protocol.md`** — 리뷰 시 지배 문서를 통째 Read 금지, `plan-search.sh section/grep`으로 **필요한 절만** 올린 뒤 코드와 대조.

## 하네스가 이미/이번에 적용한 것

| 원칙 | 적용 |
|------|------|
| 인덱스+검색 | ✅ SessionStart는 문서 **인덱스**만 주입(통째 아님) + `plan-search.sh grep` |
| 절 단위 | ✅ governing-doc·review-protocol이 `plan-search.sh section` 사용 |
| 위치 활용 | ✅ PreToolUse가 "작업 직전"(컨텍스트 끝, 강한 위치)에 주입 |
| 영속화 | ✅ findings DLT가 핵심 위반을 매 세션 재주입(압축에도 안 잃음) |

## CS 원리

**검색-증강(retrieval-augmented) + 최소 컨텍스트.** "다 넣고 모델이 알아서"가 아니라, **질의에 관련된 최소 청크만** 컨텍스트에 올리는 게 RAG의 핵심이고, lost-in-the-middle·context rot 양쪽을 동시에 줄인다. 여기선 임베딩 대신 **구조(마크다운 헤더) 기반 섹션 추출 + grep**으로 가볍게 구현했다 — 문서가 잘 구조화돼 있으면 헤더가 곧 인덱스이므로 임베딩 없이도 충분히 정밀하다. 더 키우려면 이 자리에 임베딩 검색을 끼우면 된다(같은 인터페이스).
