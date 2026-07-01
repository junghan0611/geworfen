# NOW
- Current: 리포 중심을 `ROADMAP.md`(arXiv `cs.HC` 논문 = 북극성 + 기술축)로
  정렬. botlog `20260331T123550`에 `[2026-07-01]` 논문 재개 헤딩 추가. 코드 변경 없음.
- Next: **논문 1층 세우기** — Method + 최소실험(A/B, 5지표)을 `docs/main-ko.org`
  본문에 이식(코드 아님, botlog `[2026-04-03]` 재료 이동 = arXiv 방어선). 이후
  March→July 실증 갱신 + stale 수치 자동화. 축·순서 상세는 `ROADMAP.md`.
  - 병행 관찰: 데몬이 얼마나 오래 사나 — `Restart=no`라 죽으면 그게 신호.
- Blocker: none
- Read: `ROADMAP.md`(북극성·축), `AGENTS.md`(검증·운영), botlog `20260331T123550`
  (논문 워크로그 SSOT), `docs/main.org`·`main-ko.org`(논문 본체)
- Do not touch: nixos-config 안 건드림. systemd 유닛 SSOT는 `ops/systemd/`,
  설치 위치(`~/.config/systemd/user/`)는 거기서 동기화만.

# RECENT
- [2026-07-01] **ROADMAP.md 신설** — arXiv 논문을 북극성으로, 기술축(관측성/
  시간축/데이터축)을 "논문 증거 생성기"로 정렬. `docs/main.org`가 이미 8~10쪽 ACM
  초안임을 재확인(뼈대 아님). Perplexity 논문 대조로 human-side expansion 프레임.
- [2026-07-01] **v2026.7.1** — 6/18 "자가복구"가 자해 루프였음 규명(watchdog ping이
  `timeout` 부재로 16,354회 재시작). watchdog 제거 + `Restart=no`. webtui 버전 고정.
  `AGENTS.md`에 SPA 검증법 기록.
- [2026-06-18] v2026.6.18 — (되돌림) emacs 데몬 자가복구 systemd 인프라. CalVer 전환.

# LEDGER
- geworfen은 org 데이터를 호스트 emacs 데몬에서 emacsclient로 읽는다.
  데몬 가용성 = geworfen 가용성. 운영 상세는 `ops/README.md`.
- 페이지 검증은 `curl /`이 아니라 `/api/agenda`·`/api/stats` 직접 조회(SPA).
  상세는 `AGENTS.md`.
- geworfen = 가든과 구별되는 **존재 데이터 공개 표면** = 논문(arXiv)의 살아있는
  증거면. 리포 북극성은 `ROADMAP.md`, 논문 워크로그는 botlog `20260331T123550`.
