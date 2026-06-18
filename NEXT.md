# NOW
- Current: v2026.6.18 릴리즈 완료(tag/push/GitHub release/stamp).
  사이트 정상(HTTP 200), 데몬 자가복구 systemd 정착.
- Next: 데몬 자가복구 **관찰** — `systemctl --user show agent-emacs -p NRestarts`
  추이로 "왜 죽나" 근본 원인 추적. 현재 미확정(OOM 아님), `Linger=no`가
  유력 조각이었고 linger를 켜 그 경로는 차단함. NRestarts가 자주 오르면
  그때 시각·메모리·직전 eval 확인.
- Blocker: none
- Read: `ops/README.md` (디버그 명령), `journalctl --user -u agent-emacs`
- Do not touch: nixos-config 안 건드림. systemd 유닛 SSOT는 `ops/systemd/`,
  설치 위치(`~/.config/systemd/user/`)는 거기서 동기화만.

# RECENT
- [2026-06-18] **v2026.6.18 릴리즈** — emacs 데몬 자가복구 인프라(systemd user
  service `Restart=always` + watchdog timer 1분 + `enable-linger`). SIGKILL
  6초 복구 검증. 사이트 두 번 HTTP 500(6/17 hang, 6/18 dead) 겪고 정착.
  버전 체계 SemVer 0.x → **CalVer** 전환. cron `healthcheck.sh` deprecated.
- [2026-05-24] v0.3.1 — `health` stat을 lifetract.db 실측 수면 일수로 전환.

# LEDGER
- geworfen은 org 데이터를 호스트 emacs 데몬에서 emacsclient로 읽는다.
  데몬 가용성 = geworfen 가용성. 데몬 운영 상세는 `ops/README.md` 참조.
