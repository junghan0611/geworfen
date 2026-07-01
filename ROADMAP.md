# geworfen — ROADMAP

geworfen이 **어디를 향하는가**. `NEXT.md`가 다음 한 걸음이라면 여기는 북극성.
방향 축을 담는 manual 문서다 — 릴리즈 계획도, 마감도 아니다. 순서는 우선순위가
아니라 의존 관계다.

## 정체 — geworfen은 무엇인가

가든(`notes.junghanacs.com`)이 **텍스트 지식**의 공개 표면이라면, geworfen
(`agenda.junghanacs.com`)은 **존재 데이터**의 공개 표면이다 — 시간축, agenda
스탬프, 커밋, 생체, 활동. 한 인간의 삶이 시간 위에 던져진(*geworfen*) 자국.

지금은 org-agenda day-view **하나**뿐이다. 하지만 이 표면을 개선할수록 존재
데이터를 **더 다양하게 표현**할 수 있고, 그 표현의 확장이 곧 아래 논문의
"살아있는 증거면"을 두껍게 한다.

## 북극성 — arXiv 논문

`docs/main.org`는 4월에 이미 거의 완성된 ACM 초안(8~10쪽 상당)이다. 이걸 **새로
쓰는 게 아니라 6·7월 실증으로 갱신**해 **arXiv `cs.HC`**에 올린다. 논문 제목:
*"Preparatory Practice for Human-Agent Coevolution: A Longitudinal Self-Study of
Harness Engineering."*

논문 자체가 geworfen(*던져진 것*)의 물리적 실현이다 — 코드(뷰어) + 문서(논문) +
데이터(스냅샷)를 한 리포에서 재현한다. **이 리포가 논문의 재현 패키지다.**

작업 로그(SSOT): botlog `20260331T123550` (`~/org/botlog/`, 논문 워크로그).

### arXiv까지 남은 4가지

1. **Method + 최소실험(A/B, 5지표) 본문 이식** — 1층 차가운 실증. arXiv 방어선.
   재료는 botlog `[2026-04-03]` 헤딩에 이미 있다(조건 A 날것 / 조건 B 하네스 /
   범위일탈·경계준수·연속성회복·구조화품질·자기정당화감소). 코드가 아니라 이동.
2. **March → July 실증 갱신** — entwurf, pi-shell-acp, geworfen 진화(v2026.7.1).
   4월엔 하네스가 개념에 가까웠고, 지금은 실행체가 생긴 뒤의 논문이다.
3. **stale 수치 자동화** — `1,477→1,565`일, `3,300→3,561`노트, `2,100→2,248`가든.
   org export 매크로(`{{{notes-count}}}` 등)로 갱신.
4. **잔여** — `system diagram` / `sleep figure` 2개 TODO, `main.bib` cite 실체,
   ACM sigconf → arXiv `cs.HC` 포맷 정렬.

## 기술 축 — 페이지 개선 = 표현 확장 = 증거 생성

geworfen 페이지를 키우는 일은 논문 5차원(리듬·연상·재현·투명·생체)의 증거를
만드는 일이다. 각 축이 어디에 증거를 대는지 함께 적는다.

### 축 1 — 관측성 / 신뢰성  → [투명]

- `/api/health`, 데몬 uptime·status endpoint, 죽으면 알림(Google Chat).
- `Restart=no`로 바꿔 "죽음이 진짜 신호"가 됐다(→ `ops/README.md`). geworfen이
  그 신호를 **드러내는** 표면이 되어야 한다.
- 가장 가깝고, 방금 겪은 사건의 자연스러운 연장.

### 축 2 — 시간축 확장  → [리듬 · longitudinal]

- `±14일` → 전체 히스토리. `diary.org`(18,954줄, 2022~) 통합, 과거로 걷기.
- 논문의 종단(longitudinal) 깊이가 곧 이 시간 표면이다.

### 축 3 — 데이터축 확장  → [생체 · 연상]

- agenda 너머 denote·bib·health·시간추적을 같은 표면에. "다양한 표현"의 핵심.
- 존재 데이터의 나머지 절반. 비전(README "존재 데이터 넥서스")의 종착.

### 축 4 — 아키텍처 진화  (축 2·3의 종속 결과)

- Option A → C/D. structured output(text→JSON), file-watch 자동 캐시 무효화.
- `ARCHITECTURE.md`의 "Future needs" 그대로.

## 대조 좌표 — 에이전트 확장 vs 인간 확장

Perplexity *"How AI Agents Reshape Knowledge Work"*(2026-06)와의 대조가 이
프로젝트의 좌표를 잡아준다.

| | Perplexity 논문 | geworfen / 하네스 |
|---|---|---|
| 관측 단위 | 제품 내부 세션·tool call | 가든·repo·journal·health·시간 증거 |
| 측정 대상 | agent-side expansion (26분 실행) | human-side expansion |
| 한 줄 | "더 많이 하게 됨" | "다른 인간이 되어 감" |

전자는 제품 로그로 측정되지만, 후자는 **시간축과 존재의 로그**가 있어야 겨우
보인다. geworfen은 그 후자를 보이게 만드는 표면이다.
