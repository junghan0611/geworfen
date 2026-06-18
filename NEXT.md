# NOW
- Current: v2026.6.18 release prep 커밋 완료(ops/ 자가복구 인프라 + CHANGELOG).
  사이트 정상(HTTP 200). 데몬 자가복구 systemd화 + linger 완료, kill 6초 복구 검증.
- Next: GLG 승인 시 → `git tag v2026.6.18` → push → GitHub release → agenda stamp.
- Blocker: none (태그/push는 GLG 승인 대기)
- Read: `ops/README.md` (데몬 운영 SSOT), `CHANGELOG.md`
- Do not touch: nixos-config 안 건드림. systemd 유닛 SSOT는 `ops/systemd/`,
  설치 위치(`~/.config/systemd/user/`)는 거기서 동기화만.

# RECENT
- [2026-06-18] 사이트 HTTP 500 → emacs `server` 데몬 **dead**(소켓 소멸).
  어제(6/17)는 **hang**. 원인 미확정(OOM 아님), `Linger=no`가 유력 조각.
  → systemd user `agent-emacs.service`(Restart=always) + watchdog timer(1분)
  + `enable-linger`로 자가복구화. SIGKILL 테스트 6초 복구 검증.
  유닛 사본·운영노트를 `ops/`에 기록. cron `healthcheck.sh`는 deprecated(중복).
- [2026-05-24] v0.3.1 — `health` stat을 lifetract.db 실측 수면 일수로 전환.

# LEDGER
- geworfen은 org 데이터를 호스트 emacs 데몬에서 emacsclient로 읽는다.
  데몬 가용성 = geworfen 가용성. 데몬 운영 상세는 `ops/README.md` 참조.
