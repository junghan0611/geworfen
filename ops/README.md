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

### 정본은 tmux다 (GLG 결정, 2026-09-02)

> "systemd를 지금 안정권이 아닌 상황에선 비추야. 계속 재시작할까봐. 다 꺼져도
> 되는데 이걸 geworfen에서 이맥스 tmux로 띄우고 관리하게 해야된다."

**기동·정지·상태는 `ops/tmux/agent-emacs.sh` 하나로 한다.**

```bash
./ops/tmux/agent-emacs.sh status    # 누가 소켓을 쥐고 있나 (읽기 전용)
./ops/tmux/agent-emacs.sh start     # 전용 세션 agent-emacs 에 기동 (idempotent)
./ops/tmux/agent-emacs.sh restart
./ops/tmux/agent-emacs.sh stop      # SIGTERM — 소켓 파일까지 정리된다
```

정확히 하자면 **`Restart=no` 라 유닛도 재시작하지 않는다**(2026-07-01에 watchdog과
`Restart=always`를 함께 걷어냈다 — 위 「watchdog을 뺀 이유」). 그러니 "systemd =
재시작"은 우리 유닛에 대해선 사실이 아니고, `systemctl --user show` 로 확인하면
`Restart=no / NRestarts=0` 이다. 그럼에도 tmux를 정본으로 두는 실질 이유는 셋이다:

- **죽으면 창에 그대로 남는다.** journalctl을 열지 않아도 눈에 보인다.
- **재부팅 후 자동으로 살아나지 않는다.** 사람이 의도해야 뜬다 — "안 되면 화면 안
  뜨면 된다"는 방침과 같은 방향이다.
- **상태를 추론할 필요가 없다.** `tmux attach -t agent-emacs` 하면 그 프로세스가 거기 있다.

**전용 세션이어야 하는 이유**는 2026-09-02에 실물로 나왔다 — 데몬이 GLG 작업창
(`doomemacs-config:1.1`)의 tmux scope에 얹혔고, `KillMode=control-group`이라 **그 창을
닫으면 데몬이 같이 죽는** 상태였다. 창을 쓰는 사람은 그걸 모른다. 스크립트는 이
상태를 `status`에서 경고로 잡아낸다(cgroup 이름엔 세션명이 없어 — `tmux-spawn-<uuid>`
— tmux에 직접 물어 역추적한다).

| 항목 | 상태 |
|---|---|
| `ops/tmux/agent-emacs.sh` | **정본.** 기동·정지·진단 |
| `agent-emacs.service` | **disabled**(2026-09-02). 재부팅해도 안 뜬다. 파일은 참조 구현으로 남긴다 — `Type=notify`·`Restart=no`의 근거가 거기 적혀 있고, 안정권에 들면 되돌릴 수 있다 |
| `loginctl enable-linger` | `Linger=yes`. tmux 세션이 로그아웃과 무관하게 상주 |

`ops/systemd/`는 여전히 유닛의 SSOT다. 다만 지금은 **쓰이지 않는 SSOT**다 —
되돌릴 때 읽을 것.

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

## emacsclient ↔ server 버전 스큐 — 초록불 켜진 채 내용만 깨진다 (2026-09-02)

호스트 emacs를 **30.2 → 31.1**로 올릴 때 정면으로 맞는 위험. 형제 세션
(nixos-config, garden `20260902T121754-8ec9c8`)이 오라클에서 격리 프로브로
실측해 넘겨준 사실을 여기 기록한다.

**계약 변경(upstream).** Emacs 31 `server.el`이 응답의 `server-msg-size` 청크
분할을 없애고 한 줄로 보낸다. 30.2 `emacsclient.c`는 고정 `BUFSIZ` 버퍼로 매
`recv`를 독립 메시지로 처리해 쪼개진 줄을 못 합친다 → **upstream Bug#80807**
(commits `7d07690e…` 클라이언트 선수정 / `38e704c1…` 서버 분할 제거). 우리
환경 고유 현상이 아니다.

| server | client | `(agent-org-agenda-week)` 수신 | 손상 |
|---|---|---|---|
| 30.2 | 30.2 | 25959 B | 없음 |
| 31.1 | 31.1 | 25959 B | 없음 |
| **31.1** | **30.2** | **35820 B** | `&_` 9509개 |
| 30.2 | 31.1 | 25959 B | 없음 (지원되는 방향) |

> 출처: 형제 세션 프리플라이트 실측. 이 리포에서 재측정하지 않았다(라이브 무접촉).

손상 형태 둘 — (1) 인용 누출: 공백→`&_`, 하이픈→`&-`, newline→`&n`.
(2) **stdout payload 안쪽에** `\n*ERROR*: Unknown message: ` 주입, 간격 ≈ 8192 B
(= recv 버퍼). 첫 주입 offset 8185.

### 왜 무서운가 — 우리 healthcheck가 이걸 원리적으로 못 잡는다

compose healthcheck는 `emacsclient -s server --eval '(+ 1 1)'`(응답 1 B)라
**경계를 안 넘어 항상 통과한다.** exit 0, stderr 0 B, HTTP 200 — 초록불이 다
켜진 채 agenda 본문만 깨진다. autoheal도 안 돈다.

### 8192 B는 이미 우리 일상 크기다 (여기서 실측)

"week 뷰만 넘는다"는 추정은 **틀렸다.** geworfen이 부르는 유일한 elisp는
`agent-org-agenda-day`(`agenda.clj:140`)인데, 그 응답이 이미 경계를 넘는 날이
현재 서빙 창(±14일) 안에 있다.

```bash
# 2026-09-02 13:2x KST, 라이브 데몬(30.2)에 읽기 전용 eval
emacsclient -s server --eval '(agent-org-agenda-day "2026-09-01")' | wc -c   # 8832
emacsclient -s server --eval '(agent-org-agenda-day "2026-08-30")' | wc -c   # 8643
emacsclient -s server --eval '(agent-org-agenda-day "2026-09-02")' | wc -c   # 2512  (그날 오전)
```

`/api/agenda?date=` 의 `raw` 길이로도 같은 결론 — 15일 중 2일이 8185 B 초과
(8-30: 8589 B / 9-01: 8776 B, 엔트리 49·51개). 엔트리가 많은 날이면 **오늘의
day 뷰가 그대로 깨진다.** 게다가 오늘 날짜는 하루가 갈수록 커진다.

캐시 때문에 손상이 굳는다: 오늘 60초, 과거 날짜 **1시간**(`agenda.clj:15-17`).
`eval-elisp`는 exit 0만 보고 내용을 검증하지 않는다(`emacs.clj:14-23`).

### 순서 — client-first (형제 세션 채택안, 여기 기록)

31 client는 옛 server의 `-print-nonl`을 계속 지원한다(위 표 마지막 줄).
그래서 **컨테이너를 먼저** 올린다:

1. 컨테이너 emacsclient를 31.1로 recreate (호스트 server는 아직 30.2 = 지원 방향)
2. 컨테이너 안에서 day 응답이 30↔30 baseline과 **바이트 동등**인지 확인
3. `nixos-rebuild switch` → `doom sync`(GLG 직접) → `agent-emacs.service` restart
4. 31↔31 바이트 동등 + `/api/agenda` 스모크

이 순서면 `30 client → 31 server`(깨진 방향)가 한순간도 없다.

> 마운트 수 주의: 31.1 `emacsclient`는 `libselinux`·`pcre2`가 새로 `NEEDED`라
> nix store 마운트가 2개 → 5개로 는다. 빠뜨리면 ELF 로드 실패 → healthcheck
> 사망 → autoheal 루프. 근본 대책은 해시 추적을 끝내는 것(`ROADMAP.md` 축 1).

### 현재 상태 — client-first 1단계 완료, 2단계 PASS (2026-09-02 14:0x 실측)

컨테이너는 **이미 31.1 emacsclient로 recreate 됐다**(StartedAt 13:34 KST,
RestartCount 0). 호스트 server는 아직 30.2 — 즉 지금은 `31 client → 30 server`
= **지원되는 방향**이다.

```
docker exec geworfen emacsclient --version                → 31.1
docker inspect geworfen … PATH                            → f670wz5i…-emacs-nox-31.1/bin
docker inspect geworfen … Mounts | grep nix/store         → 5개 = 세트 A 그대로
   f8q4w2hb-glibc · r8lxndj3-libselinux · jp8avbmp-glibc · f670wz5i-emacs-31.1 · xa1brw3b-pcre2
```

**2단계(바이트 동등) 통과.** 같은 30.2 server를 컨테이너 client와 호스트
client로 각각 때려서:

| 날짜 | 컨테이너 c31.1 | 호스트 c30.2 | 판정 |
|---|---|---|---|
| 2026-09-01 | 8832 B | 8832 B | ✅ 동등 |
| 2026-08-30 | 8643 B | 8643 B | ✅ 동등 |

라이브 API도 마커 0(`U+FFFD` / `&_&_` / `*ERROR*`, 09-01·08-30·09-02 3일치).
→ **남은 것은 `nixos-rebuild switch` 하나뿐이다.**

### ⚠️ 지금 상태의 단 하나의 구멍 — "컨테이너 정지 + GC" 조합

컨테이너가 31.1로 옮겨가면서 **옛 `s2j8ix…-emacs-nox-30.2`를 놓았다.** GC root를
다시 세어보면 이제 **0이다**(13:2x엔 `/proc/8659/environ` 하나였다). 즉:

- **롤백 경로가 `3a0zzkx…`(시스템 emacs) 하나뿐이다** — 그리고 그건 glibc가
  `jwg0irp5…`다. 위 「세트 B」를 그대로 써야 한다. emacs 줄만 바꾸면 부팅 불가.
- **`f670…-emacs-nox-31.1`의 유일한 GC root는 지금 돌고 있는 컨테이너 프로세스다.**
  switch를 치기 전에 컨테이너가 멈추고 9/7 GC가 돌면 31.1이 사라져 **컨테이너를
  다시 못 띄운다.** unstable overlay라 다시 받으면 해시가 달라질 수 있다.

**switch 후(2026-09-02 13:36, 세대 70) 이 구멍은 해소됐다** — `f670…-31.1` roots=8,
`s2j8ix` roots=0(컨테이너가 놓는 순간 소멸). 대신 **다른 시계가 하나 시작됐다:**

### ⏳ 세트 B(롤백)에는 유효기간이 있다

`3a0zzkx…-emacs-nox-30.2`를 지금 붙잡고 있는 것은 **전부 "과거"다**:

```
/proc/41327/exe · /maps          ← 아직 30.2 로 도는 데몬. 5단계 restart 하면 소멸
/run/booted-system → 세대 67     ← 재부팅하면 소멸
system-64 … 69-link              ← 옛 세대. nix.gc --delete-older-than 30d 대상
```

`/run/current-system`(세대 70)은 **3a0zzkx를 잡지 않는다.** 즉 롤백 경로는
"지금은 되지만 시간이 지나면 안 되는" 상태다. 없어지는 순서까지 정해져 있다 —
데몬 restart → 재부팅 → 30일 뒤 세대 정리.

31 안착 확인까지 몇 주가 걸릴 수 있고 그 사이 롤백 창이 닫히면 안 된다.
**장기 롤백 대비가 필요하면 못을 박아둔다**(세트 B 세 경로 중 emacs만 잡으면
`jwg0` glibc는 클로저로 따라온다):
```bash
nix-store --realise /nix/store/3a0zzkx505dda6kj6m18409bpd6i5z7p-emacs-nox-30.2 \
  --add-root ~/gcroots/emacs-30.2-rollback --indirect
```
필요 없어지면 심볼릭 하나 지우면 된다. 판단은 GLG.

> 참고: 지금 `/run/booted-system`(세대 67) ≠ `/run/current-system`(세대 70) —
> **재부팅 대기 상태**다. 재부팅은 `machines/oracle.nix`의 `emacs-socket-dir.service`
> (2026-09-01 부팅 경합 방지)가 실제로 먹는지 검증되는 순간이기도 하다.

(switch 전이었다면) 못 치는 경우 31.1 쪽에 박아둘 한 줄:
```bash
nix-store --realise /nix/store/f670wz5i55z7kw8dkg229wqh1psi3mp2-emacs-nox-31.1 \
  --add-root ~/gcroots/emacs-31.1 --indirect
```

### autoheal 제거 이후 — healthcheck의 의미가 바뀌었다

2026-09-02 GLG 방침으로 `autoheal=true` 라벨이 제거됐다(현재 컨테이너 Labels에
없음을 `docker inspect`로 확인). 근거가 된 사건은 자해 루프였다 — autoheal 로그에
`2026-09-01 11:45 / 11:48 / 11:50 / 11:53` **2.5분 간격 4연속 재시작**이 남아 있고,
그날은 부팅 경합으로 소켓 디렉토리가 root 소유가 된 날이다(`machines/oracle.nix`
`emacs-socket-dir.service` 주석). **원인이 호스트에 있으니 컨테이너를 아무리
재시작해도 낫지 않는다** — 2026-06/07 watchdog 자해 루프(16,354회)와 같은 계열.

**전체 이력 — 143회다.** `docker logs autoheal | grep -c "geworfen.*unhealthy - Restarting"`:

```
2026-07-01   16회   ← 이 문서가 이미 경고했던 org-agenda hang 사건 그날
2026-07-31    2회
2026-08-15   99회   ← 최대
2026-08-16   22회
2026-09-01    4회   ← 부팅 경합(소켓 dir root 소유)
```

두 가지가 읽힌다.

1. **"8/16 5시간 120회"는 날짜가 하나가 아니다** — 실제로는 **8/15 99회 + 8/16
   22회**로 밤을 넘겨 걸쳐 있다. 주된 날은 8/15다.
2. **7/1 16회는 이 문서가 맞힌 사건이다.** 위 「알려진 hang 원인」 절 끝에
   *"autoheal이 이 hang을 컨테이너 재시작으로만 대응하면 근인(데몬)을 못 고치고
   무한 루프로 원인을 가린다"* 고 그때 이미 적어놨다. **그런데 라벨은 안 뗐고**,
   6주 뒤 같은 구조가 99회로 커졌다. 경고를 문서에 적는 것과 조치하는 것은
   다른 일이라는 기록으로 남긴다.

**`restart: unless-stopped`는 유지한다 — 이 호스트에서 루프를 만든 적이 없다.**
`docker inspect geworfen --format '{{.RestartCount}}'` → **0**. 위 143회는 **전부**
autoheal이 한 것이고 docker의 재시작 정책이 문 적은 0회다. 둘은 다른 사건이다:
프로세스가 죽는 것(재부팅 복귀에 필요) vs healthcheck 실패(원인이 호스트면 재시작이
못 고침). GLG가 문제 삼은 건 후자다.

이 변화가 앞 절의 healthcheck 설계 전제를 바꾼다:

- **이전**: healthcheck 강화 → 스큐 감지 → autoheal 무한 재시작 = 자해 루프.
  그래서 강화가 위험했다.
- **지금**: autoheal이 없으므로 unhealthy는 **아무것도 재시작하지 않는다.**
  `docker ps`의 신호등으로만 남는다. 즉 `(make-string 20000 ?x)` 프로브를 넣어도
  안전하고, "안 되면 화면 안 뜨면 된다"는 방침과 정확히 맞는다.

**단, 지금 넣지 않는다.** Emacs 31 안착이 우선이고 geworfen 안정화는 그 다음이다
(GLG 방침 2026-09-02). 문안은 위에 확정해 뒀으니 그때 꺼내 쓴다.

> compose에 아직 거짓 주석이 한 줄 남아 있다 — healthcheck 바로 위의
> `# emacs 소켓 ping — 실패 시 unhealthy로 마킹되어 autoheal이 재시작`. 6줄 위에서
> "autoheal 없음"이라 해놓고 여기선 재시작한다고 말한다. nixos-config 소관이라
> 우리가 안 고친다. 형제 세션에 넘겼다.

## 배포 (재빌드) — 2026-09-02에 실제로 밟은 함정 둘

`index.html`과 서버 코드는 **GraalVM native-image 바이너리에 임베드된다.** 컨테이너의
`resources/public` 볼륨 마운트는 `io/resource` 경로에서 **읽히지 않는다** — 프런트엔드
한 글자만 고쳐도 재빌드가 필요하다.

```bash
nix develop -c ./run.sh build --force        # 2분 33초 (aarch64, Peak RSS 2.5GB)
readelf -d target/geworfen-aarch64 | grep runpath   # glibc 가 compose 마운트와 같은지
cd ~/nixos-config/docker/geworfen && docker compose up -d --force-recreate
```

**flake.lock 을 건드리지 마라.** lock 이 고정돼 있어야 재빌드해도 같은 glibc
(`jp8avbmp…`)가 나오고 compose 마운트를 안 고쳐도 된다. lock 을 올리면 해시가 바뀌어
「세트 A」5줄을 전부 다시 맞춰야 한다. (참고: `prime-agent` 는 같은 `nixos-26.05`
브랜치지만 rev 가 다르다 — 브랜치가 같다고 store path 가 같지 않다.)

### 함정 1 — `target/` 에 백업을 두면 빌드가 지운다

`build.clj:11-12` 의 `uber` 가 **가장 먼저 `clean`(= `b/delete {:path "target"}`)** 을
호출한다. 롤백용으로 `target/geworfen-aarch64.bak` 을 만들어 뒀는데 빌드가 통째로
지웠다. **백업은 `target/` 밖에 둔다** — 예: `~/geworfen-rollback/`.

### 함정 2 — `docker cp` 로는 삭제된 옛 바이너리를 못 건진다

백업이 날아갔을 때, 컨테이너는 여전히 옛 inode 로 돌고 있어 회수가 가능하다. 다만
**`docker cp` 는 bind mount 를 호스트 경로로 해석**해서 새 파일을 가져온다(크기·해시로
확인했다). 컨테이너 mount namespace 안에서 읽어야 한다:

```bash
docker exec geworfen ls -l /app/geworfen-aarch64        # 옛 크기가 보이면 살아있다
docker exec geworfen cat /app/geworfen-aarch64 > ~/geworfen-rollback/geworfen-aarch64.<날짜>
```
`docker cp` 가 가져온 것과 `docker exec cat` 이 가져온 것의 **크기가 다르다** —
전자 74516672(새것), 후자 74582280(옛것). 이 구분을 못 하면 "롤백했는데 그대로"가 된다.

### 롤백

```bash
cp ~/geworfen-rollback/geworfen-aarch64.<날짜> ~/repos/gh/geworfen/target/geworfen-aarch64
cd ~/nixos-config/docker/geworfen && docker compose up -d --force-recreate
```

## 터졌을 때 — 손으로 되살리기 (자동복구 없음이 방침이다)

데몬도 컨테이너도 자동으로 안 살아난다. 화면이 안 뜨면 그대로 두고 원인부터 본다.

```bash
# 0. 무엇이 죽었나 — 한 줄로 가른다
systemctl --user is-active agent-emacs.service      # inactive/failed → 데몬
docker ps --filter name=geworfen --format '{{.Status}}'   # 없음/unhealthy → 컨테이너
curl -s -o /dev/null -w '%{http_code}\n' https://agenda.junghanacs.com/api/stats
```

| 증상 | 원인 | 조치 |
|---|---|---|
| 데몬 inactive | 죽었다(`Restart=no`라 안 살아남) | `journalctl --user -u agent-emacs.service`로 이유 확인 후 `systemctl --user start agent-emacs.service` |
| 유닛 inactive인데 소켓은 응답 | **유닛 밖 데몬**(아래 절) | 그냥 `start` 치지 마라 — SIGTERM 정리 후 `systemctl --user start` |
| 데몬 active인데 API 500/000 | **hang** (org-agenda 프롬프트 등) | `timeout 8 emacsclient -s server --eval '(+ 1 1)'` → exit 124면 hang. 로그에서 `[R]emove from list` 확인 → `systemctl --user restart agent-emacs.service` |
| 컨테이너 없음/재시작 반복 | 마운트한 store path 실종(GC) 또는 ELF 로드 실패 | `docker logs geworfen`에 `No such file` / `cannot open shared object` → 위 「세트 A/B」로 마운트 재확인 후 `docker compose up -d --force-recreate` |
| 전부 초록인데 **내용만 이상** | **버전 스큐** (이 문서 위쪽) | `docker exec geworfen emacsclient --version` vs `emacsclient --version` 비교. 어긋나면 컨테이너 마운트를 host 세대에 맞춘다 |
| `/api/stats`의 한 항목만 0 | 데이터 레포 재클론 stale mount | `docker compose up -d --force-recreate` |

> **신호등에 구멍이 하나 있다.** autoheal이 없어진 뒤 사람이 보는 신호는
> `docker ps`의 health인데, **크래시 루프는 health가 아니라 `Status`로 나타난다** —
> 마운트한 store path가 사라지거나 ELF 로드가 실패하면 컨테이너가 running에
> 도달하지 못해 healthcheck 자체가 안 돈다. `unhealthy`만 찾으면 놓친다. 한 줄로 가른다:
> ```bash
> docker inspect geworfen --format '{{.State.Status}} restarts={{.RestartCount}} health={{.State.Health.Status}}'
> ```
> `restarts`가 0보다 크고 계속 오르면 크래시 루프다(로그는 `docker logs geworfen`).
> `running`인데 `unhealthy`면 데몬 쪽이다. 평상시 값은 `running restarts=0 health=healthy`.

### 관측 — `pgrep -f` 는 자기 자신을 잡는다 (2026-09-02, 하루에 세 번 밟음)

`ops/tmux/agent-emacs.sh` 의 `daemon_pids` 가 명령줄 매칭에 더해 **실행 바이너리가
emacs 인지** 확인하는 이유다. 그날 같은 함정을 세 번 밟았다:

1. `pgrep -f emacs-async-comp` 가 자기 셸을 세어 "native-comp 진행 중 4개"로 오독
   (nixos-config 세션). 실제로는 이미 끝나 있었다.
2. 내가 같은 명령으로 "2개"를 얻음. `ps -eo cmd | grep "[e]macs-async-comp"` 로
   다시 재서 0을 확인했다.
3. **위험했던 것** — 데몬 이전 직전 `pgrep -f -- '--daemon=server'` 가 3개를 반환했다
   (데몬 1 + 그 문자열을 명령줄에 담은 셸 자식 2). 스크립트가 그대로
   `kill -TERM $pids` 를 쳤으면 남의 프로세스를 죽였다. 실행 전에 잡았다.

교훈은 도구가 아니라 형태다: **관측 명령이 관측 대상 집합에 자기를 넣는다.**
`grep "[e]macs"` 같은 브라켓 트릭이나 `/proc/<pid>/exe` 확인으로 걸러야 한다.

### 관측 — eln 로드 경로는 재기동만으로 안 고쳐진다 (2026-09-02)

31.1 데몬이 30.2 eln 만 들고 있던 문제는 "기동이 1초 빨라 31.1 디렉토리를 못 봤다"로
진단됐고 재기동으로 해소될 것으로 예상됐다. **재기동 후 실측하면 절반만 맞다:**

```
native-comp-eln-load-path  (재기동 후)
  1. …/cache/eln/30.2-144d75d9     ← 첫 항목이 여전히 30.2
  2. …/cache/eln/31.1-c13bfc9a     ← 들어오긴 했다
  3~5. 30.2-…
```

31.1 디렉토리가 경로에 **들어왔지만 첫 항목이 아니다.** Emacs 는 새로 컴파일한 `.eln`
을 첫 항목 아래에 쓰므로, 31.1 캐시는 여전히 제대로 안 찬다. 원인은 타이밍이 아니라
**순서**다 — `~/.doom.d/bin/agent-server.el:107-114` 가
`(directory-files doom-eln-dir t "^[0-9]" t)`(nosort)로 긁어 `add-to-list` 하므로
순서가 파일시스템 순서에 좌우된다.

**이 리포 소관이 아니다**(host `doomemacs-config`). 근본 수정은 `^[0-9]` 정규식을
손보는 게 아니라 `emacs-version` 으로 거르는 것이라는 게 우리 의견 — `build-<version>`
selector 를 mtime 으로 고르는 것과 같은 계열의 결함이다. 기능 영향은 없다
(`.elc` 폴백, agenda 정상·마커 0). native-comp 이득만 못 받는다.

### ⚠️ 유닛 밖에서 도는 데몬 — 지금(2026-09-02) 이 상태다

**복구 절차를 쓰기 전에 이것부터 본다.** 유닛이 `inactive` 인데 소켓이 살아 답하는
상태가 있다. 그때 `systemctl --user start` 를 그냥 치면 소켓이 이미 점유돼 충돌한다.

```bash
systemctl --user is-active agent-emacs.service     # inactive 인데
emacsclient -s server --eval '(+ 1 1)'             # → 2 가 나오면 = 유닛 밖 데몬
```

2026-09-02 이관에서 실제로 이 상태가 됐다. `doom sync` 뒤 데몬을 systemd 밖에서
손으로 띄웠고(유닛 종료 1초 뒤 기동), 실측하면 이렇다:

```
/proc/845830/cmdline → emacs --init-directory=/tmp/agent-emacs-init \
                         --daemon=server --load ~/.doom.d/bin/agent-server.el
PPID=1                                    ← 고아처럼 보이지만
cgroup = …/tmux-spawn-b1b4719c-….scope    ← tmux pane 의 scope 안에 있다
systemctl --user show <scope> -p KillMode → control-group
```

**PPID=1 이라고 안전한 게 아니다.** cgroup 이 tmux scope 이고 `KillMode=control-group`
이므로, **그 pane 이 닫히면 systemd 가 cgroup 전체를 죽인다.** 같은 scope 안에 있는 것:

```
799511  -bash                     ← doomemacs-config:1.1 pane 의 셸(사람이 쓰는 창)
845830  emacs --daemon=server     ← agenda + openclaw 백엔드
845920  emacs --daemon=pi         ← pi 하네스용
```

즉 **사람이 그 창에서 `exit` 한 번 치면 데몬 둘이 같이 죽는다.** 유닛은 inactive 라
systemd 는 모르고, `Restart=no` 는 유닛의 정책이라 유닛 밖 프로세스에는 적용되지
않는다. 자동으로 살아나는 경로가 하나도 없다.

**정리 절차** (다운타임 수 초):
```bash
# 1. SIGTERM 으로 죽인다 — SIGKILL 은 소켓 파일을 남겨 다음 기동을 방해한다
#    (server.el 은 정상 종료 시 delete-file server-file 을 한다)
kill -TERM <server 데몬 PID>
# 2. 소켓이 정리됐는지 확인
ls /run/user/1000/emacs/          # server 가 없어야 한다
# 3. 유닛으로 되돌린다 (여기서부터 Type=notify / Restart=no 가 다시 의미를 가진다)
systemctl --user start agent-emacs.service
systemctl --user is-active agent-emacs.service   # active
emacsclient -s server --eval '(+ 1 1)'           # 2
```

> `--daemon=pi` 는 pi 하네스 소관이라 이 리포가 정하지 않는다. 다만 **같은 scope 에
> 있어 같이 죽는다**는 사실은 그쪽도 알아야 한다.

> 타이밍: `doom sync` 직후의 async native-comp 가 끝난 뒤에 한다. 돌고 있으면
> `ps -eo cmd | grep "[e]macs-async-comp"` 에 잡힌다. 캐시가 덥혀진 뒤라야 재기동이
> 콜드 7초가 아니라 2.5초로 끝난다.

데몬 기동 후 확인은 한 줄:
```bash
emacsclient -s server --eval '(+ 1 1)'   # → 2
curl -s 'https://agenda.junghanacs.com/api/agenda' | head -c 200
```

### 손상은 예외를 던지지 않는다 — JVM은 조용히 U+FFFD로 바꾼다 (2026-09-02 확정)

형제 세션이 31 server → 30 client 방향을 직접 관측해 확인해줬다: 9/1 day 응답이
**8210 B → 8719 B**로 부풀고 `&_` 448개·`&-` 30개, offset 6732에 에러문 주입,
그리고 **recv 경계가 한글 3바이트 문자를 2바이트에서 잘라 무효 UTF-8이 된다.**

그쪽에서 python `json.dumps`가 `UnicodeDecodeError`로 죽었다고 보고됐는데,
**우리 스택은 그렇게 동작하지 않는다. 이 기대를 그대로 옮기면 안 된다.**

- `clojure.java.shell/sh`의 `:out-enc` 기본값은 `"UTF-8"`
  (`clojure-1.12.0.jar` → `clojure/java/shell.clj:47`).
- 디코드는 `clojure.java.io/copy`의 `do-copy [InputStream Writer]`가 하고, 그 구현은
  **`InputStreamReader`** 다(`clojure/java/io.clj:313-320`). `InputStreamReader`의
  계약은 malformed input을 **REPLACE** — 예외가 아니라 U+FFFD 치환이다.

→ **`exit=0`, `err=""`, 유효한 JSON, 내용만 깨진 채로 캐시에 굳는다.** 예외가 안
나므로 500도 안 뜬다. 지금까지 나온 실패 모드 중 가장 조용하다.

같은 `InputStreamReader` 때문에 **정상 응답은 1024자 버퍼 경계에서 안 깨진다**
(디코더가 부분 시퀀스를 다음 read로 이월한다). 즉 U+FFFD가 보이면 그건 버퍼
아티팩트가 아니라 **진짜 무효 바이트**다 — 그래서 탐지 마커로 신뢰할 수 있다.

### `eval-elisp` 무결성 가드 — 마커 확정

```clojure
;; 셋 다 라이브 페이로드(2026-08-25 / 08-30 / 09-01 / 09-02)에서 출현 0 확인.
(defn- corrupted? [^String out]
  (or (str/includes? out "\ufffd")                     ; 무효 UTF-8 → REPLACE 흔적
      (str/includes? out "*ERROR*: Unknown message:")   ; 프로토콜 에러문 주입
      (re-find #"&_&_" out)))                           ; 인용 누출(연속 2개 = 공백 2칸)
```

`&_`를 **연속 2개**로 보는 이유: 단독 `&_`는 org 본문(URL 등)에 우연히 나올 수
있지만, 손상 시엔 448개가 나오므로 연속이 반드시 생긴다. 오탐을 0으로 두면서
탐지력은 유지된다.

던지는 자리는 `eval-elisp` 안 — `cache-put!`(`agenda.clj:140-143`) **앞**이라
오염이 캐시에 굳지 않는다. 조용한 손상이 500으로 드러나는 것이 이 리포의 운영
철학(위 「자동 복구 안 함」)과 같은 방향이다. **재빌드가 필요하므로 GLG 승인 후.**

### compose healthcheck 교체 문안 — 기준값 실측 확정

현행 `(+ 1 1)`은 응답 1 B라 경계를 못 넘어 **원리적으로** 이 손상을 못 잡는다.
`(make-string 20000 ?x)`로 바꾼다 — org를 안 건드리고 전송 계약만 검증한다.

```yaml
healthcheck:
  test: ["CMD-SHELL", "[ \"$(emacsclient -s server --eval '(make-string 20000 ?x)' 2>/dev/null | wc -c)\" = 20003 ] || exit 1"]
  interval: 60s
  timeout: 10s
  retries: 3
  start_period: 30s
```

기준값은 **`wc -c` 기준 20003**(20000 + 따옴표 2 + 개행 1)으로 실측했다. 셸
명령치환 `$(...)`은 개행을 먹으므로 `${#out}` 방식이면 **20002**다 — 두 수를
섞지 마라(`scripts/emacs-skew-check.sh`는 후자라 20002가 맞다).

비용: 왕복 **8 ms** 실측, 60초마다 20 KB. 20000자는 8185·16404 두 경계를 모두
넘어 단일 경계 우연 통과를 막는다. hang도 `timeout 10s`로 그대로 잡힌다.
**한계는 명시해 둔다 — 이건 전송을 검증하지 agenda 내용을 검증하지 않는다.**

> 크기가 경계 근처에서 진동한다는 점이 이 사고의 성격을 정한다. 같은 9/1
> 응답을 13:2x에 8776 B, 13:5x에 8155 B로 쟀다(org가 바뀌는 중). **어떤 날은
> 깨지고 어떤 날은 멀쩡한 간헐 장애**로 나타난다 — 그래서 사람이 눈으로 잡기
> 가장 어렵고, 그래서 프로브가 데이터가 아니라 고정 크기여야 한다.

### 31.1 전환 준비물 — 마운트 세트와 GC 마감 (2026-09-02 실측)

31.1은 **이미 store에 있다**(`nixos-config` `1c4ecbf` "move every device to Emacs
31.1 via the unstable overlay"). 시스템에 switch만 안 쳤다 —
`readlink -f /run/current-system/sw/bin/emacsclient` → `3a0zzkx…`(30.2).

컨테이너에 넣을 경로는 `ldd`가 아니라 **`readelf -p .interp` + RUNPATH**로 정해야
한다. `ldd`는 자기 로더(`RTLDLIST`)를 INTERP 자리에 끼워 보여줘서 한 번 오독이
났던 자리다.

| | INTERP | RUNPATH |
|---|---|---|
| 앱 `target/geworfen-aarch64` | `jp8avbmp…-glibc-2.42-67` | `jp8avbmp…` + `m3v5jbl1…-gcc-15.2.0-lib` |
| **현재 마운트** `s2j8ix…-emacs-nox-30.2` | `jp8avbmp…` | `jp8avbmp…` |
| **시스템** `3a0zzkx…-emacs-nox-30.2` | **`jwg0irp5…`** | **`jwg0irp5…`** |
| **목표** `f670wz5i…-emacs-nox-31.1` | `f8q4w2hb…-glibc-2.42-67` | `r8lxndj3…-libselinux-3.10` + `f8q4w2hb…` |

읽은 곳: `readelf -p .interp <bin>/bin/emacsclient` / `readelf -d … | Library runpath`.

**세트 A — 31.1 전환(5개).** `jp8avbmp…-glibc`(앱) · `f8q4w2hb…-glibc`(client) ·
`r8lxndj3…-libselinux-3.10` · `xa1brw3b…-pcre2-10.47`(libselinux가 끌고 옴) ·
`f670wz5i…-emacs-nox-31.1`. `jwg0irp5…`는 **이 세트에 들어가지 않는다**(ldd 아티팩트).

**세트 B — 롤백/폭탄제거(3개).** 지금 마운트된 `s2j8ix…`는 GC root가
`/proc/<pid>/environ` 하나뿐이라 컨테이너를 recreate 하는 순간 보호가 사라진다.
되돌아갈 곳은 시스템이 잡고 있는 `3a0zzkx…`인데, **그건 glibc가 `jwg0irp5…`다.**
`s2j8ix → 3a0zzkx`로 바꾸면서 glibc 줄을 그대로 두면 **ELF 로드 실패**한다.
세트 B = `jp8avbmp…`(앱) + `jwg0irp5…`(client) + `3a0zzkx…-emacs-nox-30.2`.

**GC 마감 — 2026-09-07 00:00 KST (`nix-gc.timer`, weekly, `--delete-older-than 30d`).**
`nix-store --query --roots` 로 센 root 수:

```
f670wz5i…-emacs-nox-31.1        roots=0   ← 고아
r8lxndj3…-libselinux-3.10       roots=0   ← 고아
f8q4w2hb…-glibc-2.42-67      roots=2942
xa1brw3b…-pcre2-10.47        roots=2924
jwg0irp5…-glibc-2.42-67      roots=2942
jp8avbmp…-glibc-2.42-67        roots=12
```

31.1 본체와 libselinux가 **아무도 안 잡고 있다.** GC가 먼저 돌면 다시 받아야
하는데, unstable overlay는 rev가 움직이면 **해시가 달라진다** — 지금 정리 중인
compose 5줄이 통째로 무효가 된다. switch 전에 GC가 오면 그게 손실이다.
switch를 9/7 전에 못 치면 root부터 박아 둔다(예:
`nix-store --realise <경로> --add-root ~/gcroots/emacs31 --indirect`).

### 데몬 재기동 창 — autoheal과 겹치지 않게

`agent-emacs.service`는 `Type=notify`다. 소켓이 준비된 뒤에야 active가 되므로
geworfen healthcheck가 아직 없는 소켓을 때리지 않는다.

**출처 — 이건 드리프트가 아니라 의도된 미결이었다.** 2026-09-01에 *라이브에만*
`Type=simple → Type=notify`를 넣고 이 리포로의 회수를 미뤄뒀고, 그 사실이
`nixos-config` `NEXT.md:246`에 열린 항목으로 등재돼 있었다 — *"geworfen 리포에는
아직 안 넣었다 — 그쪽에서 다시 배포하면 조용히 되돌아간다."* 설치본 mtime
`2026-09-01 22:06:19`이 그날 데몬 기동 시각(22:06:28)과 9초 차로 일치한다.
2026-09-02에 이 리포로 회수하면서 그 항목이 닫혔다. `Restart=no`는 의도적
fail-stop이라 함께 건드리지 않았다.

근거 실측 두 가지를 구분해서 적어둔다 — **다른 것을 잰 수치다**:

- **기동 ~7초** — 설치본 주석의 값. 데몬이 straight `build-*`를 훑는 콜드 기동.
  `Type=simple`이면 이 7초 동안 active로 표시되고, 그 사이 healthcheck가 빈
  소켓을 때린다. 이게 `notify`로 바꾼 이유다.
- **`systemctl restart`가 2557 ms 블록 후 반환** — `notify` 적용 후 실측
  (nixos-config 세션). 소켓이 **준비된 뒤에** 반환한다는 확인이지 콜드 기동
  시간이 아니다.

31.1에서도 `Type=notify`는 안전하다: `readelf -d <emacs>/bin/emacs | grep NEEDED`가
30.2·31.1 **양쪽 다** `libsystemd.so.0`을 포함하고, 바이너리 문자열에 `LIBSYSTEMD`가
있다(2026-09-02 확인). 이게 아니었으면 `TimeoutStartSec=120` 만료로 데몬이 아예
못 뜨고 agenda가 전면 500이었다.

타이밍: healthcheck가 unhealthy로 가기까지 60s×3 ≈ 180s > `TimeoutStartSec=120`
이라 정상 기동이면 autoheal은 안 돈다. 다만 31.1 첫 기동은 straight/native-comp
재컴파일이 붙어 콜드 7초보다 길어질 수 있으므로 **`doom sync`를 데몬 restart보다
먼저** 끝낸다(이 순서를 지키면 restart는 캐시가 덥혀진 뒤의 2.5초짜리가 된다).

## 정정 — "데몬 재시작이 소켓 디렉토리 inode를 바꾼다"는 설명은 틀렸다 (2026-09-02)

`nixos-config` `docker/geworfen/docker-compose.yml` 헤더(commit `404bd81`,
2026-04-26)의 「사고 회복 메모」는 이렇게 적혀 있다:

> 호스트 emacs daemon 재시작 시 tmpfs `/run/user/1000/emacs/`가 새 inode로
> 재생성되면서 컨테이너 안의 마운트가 stale 상태가 된다.

**upstream 소스로 반증된다.** `server.el`의 소켓 디렉토리 삭제는 `/tmp/`
아래일 때만 실행된다(bug#44644 가드):

```elisp
;; server.el:665-679  (server-stop)
(delete-file server-file)
;; Also delete the directory that the server file was created in -- but only in /tmp
(when (equal (file-name-directory (directory-file-name (file-name-directory server-file)))
             "/tmp/")
  (ignore-errors (delete-directory (file-name-directory server-file))))
```

> 읽은 곳: `/nix/store/3a0zzkx…-emacs-nox-30.2/share/emacs/30.2/lisp/server.el.gz:665-679`
> (= 현재 시스템 프로파일 emacs). `delete-directory` 출현은 그 파일에서 **이 한 곳뿐**.

우리 소켓 디렉토리는 `/run/user/1000/emacs`라 부모가 `/tmp/`가 아니다. 즉
**평범한 데몬 restart는 디렉토리를 지우지 않고 소켓 파일만 바꾼다.**
`server-ensure-safe-dir`(`server.el:557-568`)도 디렉토리가 없을 때만 만든다.
현장 관측도 일치: 호스트 `/run/user/1000/emacs` inode = 64로, 그 디렉토리를
bind mount 한 컨테이너들과 동일(형제 세션 측정).

그럼 4월 사고는 무엇이었나 — **1차(4/24)는 inode가 아니라 소켓 이름 문제였다.**
geworfen `1b5270f`(2026-04-24): "호스트 emacs daemon이 `--daemon=server`로
리네이밍되어 Geworfen이 500 응답". `404bd81`은 이걸 4/26과 "동일 패턴"으로
묶었는데, 근거 기록이 서로 다른 원인을 가리킨다.

**남는 진짜 inode 경로는 데몬 restart가 아니라 런타임 디렉토리 재생성이다:**

- 로그아웃/세션 종료로 `user-runtime-dir@1000`이 `/run/user/1000` tmpfs를
  내리면 그 안의 모든 inode가 사라진다. 2026-04 당시는 `enable-linger`가
  **아직 없었다**(6/18 도입, `CHANGELOG.md` [2026.6.18]) — 데몬이 로그인 세션에
  묶여 있던 시기다(이 문서 마지막 절).
- 부팅 시 docker가 emacs보다 먼저 bind source를 만들어 버리는 경로
  (`nixos-config` `machines/oracle.nix` `emacs-socket-dir.service` 주석,
  2026-09-01 재부팅에서 실제 발생).

→ **수선 필요(이 리포 밖):** compose 헤더의 원인 문장을 "데몬 재시작"이 아니라
"런타임 디렉토리 재생성(로그아웃/부팅 경합)"으로 고쳐야 한다. `nixos-config`는
이 리포가 건드리지 않는다(`AGENTS.md`) — 형제 세션에 넘긴다.

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
