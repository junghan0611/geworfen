# geworfen 운영 노트 — emacs `server` 데몬

geworfen은 org-agenda 데이터를 **호스트의 emacs `server` 데몬**에서
`emacsclient`로 가져온다 (`geworfen.emacs/eval-elisp`). 따라서 이 데몬이
죽거나 멈추면 geworfen의 모든 페이지가 HTTP 500을 던진다.

```
[Docker: geworfen] → emacsclient (Nix store mount)
  → Unix socket /run/user/<uid>/emacs/server
    → [Host: emacs --fg-daemon=server --load ~/.doom.d/bin/agent-server.el]
```

## 운영 철학 — 자동 복구 안 함 (2026-07-01~)

이 서버는 openclaw 백엔드이자 핵심 개인 서버다. 데몬이 죽으면 **문제가
즉시 드러나야** 한다(agenda 엔트리가 사라진다). 자동 재시작으로 증상을
숨기지 않는다. 목표는 자동 부활이 아니라, **근본을 고쳐 한두 달씩 안 죽는
것**이다. 죽으면 그대로 두고, 사람/에이전트가 원인을 보고 나서 켠다.

| 유닛 | 동작 |
|---|---|
| `agent-emacs.service` | `Restart=no`. systemd가 데몬 PID를 소유하되 죽으면 **자동 부활 없음** |
| `loginctl enable-linger` | 로그아웃·세션 종료와 무관하게 부팅 시 상주 |

`ops/systemd/`가 유닛의 SSOT — 수정은 여기서 하고 설치 위치로 동기화한다.

### watchdog을 뺀 이유 (2026-06-18 → 2026-07-01)

6/18~7/1 동안 `agent-emacs-watchdog.timer`(1분마다 ping → 무응답이면
재시작)를 뒀는데, 이게 **자가복구가 아니라 자해 루프**였다. ping 명령이
`timeout 5 emacsclient ...` 였는데 systemd `--user` 서비스 PATH엔 `timeout`
바이너리가 없어 매번 `command not found`(exit 127)로 떨어졌고, `|| restart`가
발동해 **emacs에 ping을 던져본 적도 없이** 멀쩡한 데몬을 매 1분 죽였다.
누적 **16,354회** 재시작. 진짜 다운은 없었는데 로그만 오염됐다. 제거했다.

`agent-emacs.service`의 `Restart=always`도 같은 이유로 뺐다 — 자동 부활은
"점점 더 안 죽는다"는 목표와 반대로, 죽는 신호를 지운다.

## 설치 / 동기화

```bash
# repo → 설치 위치 동기화
cp ops/systemd/agent-emacs.service ~/.config/systemd/user/
loginctl enable-linger "$USER"          # 1회, 세션 무관 상주
systemctl --user daemon-reload
systemctl --user enable --now agent-emacs.service
```

> 유닛의 경로(`/home/junghan`, `/run/current-system/sw/bin/...`)는 junghan의
> NixOS 환경 기준이다. 다른 환경에서는 경로를 맞춰라.

## 죽었을 때 — 다시 켜기

```bash
systemctl --user start agent-emacs.service      # 수동 기동
/run/current-system/sw/bin/emacsclient -s server --eval '(+ 1 1)'   # 응답 확인 → 2
```

## 알려진 hang 원인 — org-agenda 대화형 프롬프트 (2026-07-01)

데몬이 죽는 게 아니라 **살아서 멈추는(hang)** 대표 케이스. `org-agenda-files`에
리네임·삭제된 파일이 남으면 `org-check-agenda-file`이 헤드리스 데몬에
`[R]emove from list or [A]bort?`를 묻는데, `--fg-daemon`엔 답할 터미널이 없어
**영구 hang** → geworfen의 emacsclient 호출이 블록 → 전 페이지 500/000.

- 2026-07-01 사례: botlog 논문 파일 제목 변경 → denote 리네임 → org-agenda가
  옛 이름을 stale로 물고 프롬프트 → 데몬 hang → agenda 다운.
- **근본 수정은 이 repo가 아니라 host `doomemacs-config/bin/agent-server.el`**:
  `(setq org-agenda-skip-unavailable-files t)` — 없는 파일을 프롬프트 대신 skip.
- 진단: `timeout 8 emacsclient -s server --eval '(+ 1 1)'` → exit 124면 hang.
  `journalctl --user -u agent-emacs.service`에서 `[R]emove from list` 확인.
- 복구: `systemctl --user restart agent-emacs.service` → `docker restart geworfen`.

> autoheal이 이 hang을 **컨테이너 재시작으로만** 대응하면 근인(데몬)을 못 고치고
> 무한 루프로 원인을 가린다. 데몬 자체를 재기동해야 한다.

## 디버그 — "왜 죽었나"

```bash
journalctl --user -u agent-emacs.service        # 데몬이 언제·왜 죽었는지
systemctl --user show agent-emacs -p NRestarts   # 재시작 횟수 (Restart=no면 0 유지)
loginctl show-user "$USER" -p Linger             # Linger=yes 여야 함
```

`Restart=no`라 `NRestarts`는 사람이 켜지 않는 한 오르지 않는다. 데몬이
죽어 있으면 그 시각·메모리·직전 eval을 보고 근본 원인을 추적한 뒤 켠다.
현재까지 OOM 증거는 없다. `Linger=no`였던 것이 과거 한 조각(데몬이 로그인
세션에 묶여 세션 종료 시 함께 죽음) — linger를 켜 이 경로는 차단했다.
