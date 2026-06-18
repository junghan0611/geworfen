# geworfen 운영 노트 — emacs `server` 데몬 자가복구

geworfen은 org-agenda 데이터를 **호스트의 emacs `server` 데몬**에서
`emacsclient`로 가져온다 (`geworfen.emacs/eval-elisp`). 따라서 이 데몬이
죽거나 멈추면 geworfen의 모든 페이지가 HTTP 500을 던진다.

```
[Docker: geworfen] → emacsclient (Nix store mount)
  → Unix socket /run/user/<uid>/emacs/server
    → [Host: emacs --fg-daemon=server --load ~/.doom.d/bin/agent-server.el]
```

## 자가복구 구조 (systemd user, 2026-06-18~)

nixos-config를 건드리지 않고 `~/.config/systemd/user/` 에 명령형으로 설치한다.
이 디렉토리(`ops/systemd/`)가 유닛의 SSOT — 수정은 여기서 하고 동기화한다.

| 유닛 | 동작 | 잡는 케이스 |
|---|---|---|
| `agent-emacs.service` | `Restart=always`, 2초 후 재기동. systemd가 데몬 PID를 소유 | **dead** (소켓 소멸) |
| `agent-emacs-watchdog.timer` | 1분마다 ping, 무응답이면 `systemctl restart` | **hang** (소켓은 있으나 무응답) |
| `loginctl enable-linger` | 로그아웃·세션 종료와 무관하게 데몬 상주 | 세션 종료로 인한 죽음 |

## 설치 / 동기화

```bash
# repo → 설치 위치 동기화
cp ops/systemd/*.service ops/systemd/*.timer ~/.config/systemd/user/
loginctl enable-linger "$USER"          # 1회, 세션 무관 상주
systemctl --user daemon-reload
systemctl --user enable --now agent-emacs.service
systemctl --user enable --now agent-emacs-watchdog.timer
```

> 유닛의 경로(`/home/junghan`, `/run/current-system/sw/bin/...`)는 junghan의
> NixOS 환경 기준이다. 다른 환경에서는 경로를 맞춰라.

## 디버그 — "왜 죽었나"

```bash
journalctl --user -u agent-emacs.service        # 데몬이 언제·왜 죽었는지
journalctl --user -u agent-emacs-watchdog        # hang 감지 이력
systemctl --user show agent-emacs -p NRestarts   # 재시작 횟수 누적
loginctl show-user "$USER" -p Linger             # Linger=yes 여야 함
```

`NRestarts`가 자주 오르면 그때의 시각·메모리·직전 eval을 보고 근본 원인을
추적한다. 현재까지: OOM 증거 없음. 어제(6/17) hang, 오늘(6/18) dead로 증상이
달라 단일 원인 미확정. `Linger=no` 였던 것이 유력한 한 조각(데몬이 로그인
세션에 묶여 세션 종료 시 함께 죽음) — linger를 켜 이 경로는 차단했다.

## Deprecated

`~/.doom.d/bin/agent-server-healthcheck.sh` (cron `*/5` 용)는 이 systemd
구조로 역할이 넘어가 중복이다. cron에 등록된 적은 없었다.
