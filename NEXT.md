# NOW
- Current: v2026.7.1 릴리즈 완료(tag/push/GitHub release/stamp) — 자가복구
  (watchdog) 회수, `Restart=no`, webtui CDN 버전 고정, `AGENTS.md` 신설.
  사이트 정상(HTTP 200), 데몬 active.
- Next: **데몬이 얼마나 오래 사나 관찰**. `Restart=no`라 죽으면 그대로 멈추고
  agenda가 빈다 → 그때 `journalctl --user -u agent-emacs.service`로 시각·직전
  eval·메모리를 잡아 근본 원인 추적 후 수동 기동. 목표는 한두 달 무중단.
- Blocker: none
- Read: `AGENTS.md`(페이지 검증·운영 철학), `ops/README.md`(디버그 명령)
- Do not touch: nixos-config 안 건드림. systemd 유닛 SSOT는 `ops/systemd/`,
  설치 위치(`~/.config/systemd/user/`)는 거기서 동기화만.

# RECENT
- [2026-07-01] **v2026.7.1** — 6/18 "자가복구"가 자해 루프였음을 규명.
  watchdog ping이 `timeout` 바이너리 부재(systemd PATH)로 exit 127 →
  멀쩡한 데몬을 16,354회 재시작. watchdog 제거 + `Restart=no`로 전환.
  webtui `@latest` → `@0.1.9`/`@0.0.5` 고정. `AGENTS.md`에 SPA 검증법 기록.
- [2026-06-18] v2026.6.18 — (되돌림) emacs 데몬 자가복구 systemd 인프라.
  버전 체계 SemVer 0.x → CalVer 전환.

# LEDGER
- geworfen은 org 데이터를 호스트 emacs 데몬에서 emacsclient로 읽는다.
  데몬 가용성 = geworfen 가용성. 운영 상세는 `ops/README.md`.
- 페이지 검증은 `curl /`이 아니라 `/api/agenda`·`/api/stats` 직접 조회.
  `/`는 JS로 엔트리를 채우는 SPA라 curl엔 비어 보인다. 상세는 `AGENTS.md`.
