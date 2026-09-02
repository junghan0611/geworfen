# NOW
- Current: **논문 산출 파이프 이식** (8442ab8 push). `docs/main.*` →
  `docs/paper/geworfen.*` 재편, org→html5 pandoc(`make html`) 추가, 생성물
  gitignore(재현 세트만). `AGENTS.md`에 이웃리포 분업 하드룰. 봇로그 갱신.
  사이트 정상(HTTP 200).
- Next: **논문 1층 세우기** — Method + 최소실험(A/B, 5지표)을 `docs/paper/geworfen-ko.org`
  본문에 이식(코드 아님, botlog `[2026-04-03]` 재료 이동 = arXiv 방어선). 이후
  March→July 실증 갱신 + stale 수치 자동화. 축·순서 상세는 `ROADMAP.md`.
- Watch: jacobian-lens 산업계 서베이 나오면 **리뷰**(아래 LEDGER) → 논문 산업계
  배경으로 소비. 연구 노동은 jacobian, geworfen은 소비만.
- Watch: `junghan0611/timeline` (2026-07-13 생김) — 인간 시간축 계측기. **관찰만,
  단방향.** 저기로 요청·지시·수치 주문 금지 = 맹검 유지 = 데이터 무결성 조건.
  왜 그런지와 논문이 가져올 넷은 `ROADMAP.md`「관측 규율」.
- Blocker: none — **대기 모드다.** oracle Emacs 31.1 이관 중이고 geworfen은
  **손대지 않고 띄워둔다.** GLG 방침(2026-09-02): *"이번에 31로 업하고 geworfen도
  원래 하던 대로 손으로 되살리는 것부터. 31 안착해서 에이전트들과 openclaw 안에서
  문제없이 유지되면 그때 geworfen 안정화."* 지금은 **힣의 Emacs 31 안정화가 우선**
  이고 소통창구(openclaw)가 깨지면 안 된다. 터지면 부른다 — 그때 쓸 절차는
  `ops/README.md`「터졌을 때 — 손으로 되살리기」.
- 자동복구 금지(GLG 방침): *"꺼지면 자동복구는 일단 no. 계속 재시도하면서 시스템
  잡아먹는 걸 원치 않는다. 안 되면 화면 안 뜨면 된다."* 데몬 `Restart=no` 유지,
  컨테이너 `autoheal` 라벨 제거됨(2026-09-02). **healthcheck는 복구 트리거가 아니라
  신호등이다.** 되살리는 건 사람이 한다.
- 31 이관 현황(2026-09-02 14:0x 실측): 컨테이너는 **이미 31.1 client로 recreate 완료**
  (마운트 5개 = 세트 A), 호스트 server는 아직 30.2 = 지원 방향. 바이트 동등 통과
  (09-01·08-30 각각 8832/8643 B, 컨테이너 = 호스트). 라이브 마커 0. **남은 건 switch 하나.**
- 구멍 하나만 지켜본다: `f670…-emacs-nox-31.1`의 GC root가 **지금 도는 컨테이너
  프로세스뿐**이다. switch 전에 컨테이너가 멈춘 채 9/7 GC(주간)가 지나면 다시 못 띄운다.
  switch를 치면 자동 해소, 못 치면 `nix-store --realise … --add-root` 한 줄.
  롤백 경로는 이제 `3a0zzkx`(시스템) 하나뿐이고 **glibc가 `jwg0`이다**(세트 B).
- **footer 배포 완료 (2026-09-02 14:4x, GLG 지시).** 라이브 확인: `id="footer"` 1,
  `/api/stats` → `versions {emacs-server 31.1, emacs-client 31.1}`(스큐 없음),
  agenda 데이터 배포 전후 **완전 동일**(09-01 8155/44, 08-30 8293/42, 마커 0),
  HTTP 200 0.031s. 롤백본 `~/geworfen-rollback/geworfen-aarch64.20260713`.
  빌드 함정 둘은 `ops/README.md`「배포(재빌드)」에 기록 — `target/`에 백업 두면
  `build.clj` clean 이 지운다 / `docker cp` 는 bind mount 를 호스트 경로로 읽어
  옛 바이너리를 못 건진다(`docker exec cat` 써야 한다).
- (원문 기록) footer 설계 — 배포 전 메모:
  페이지 하단에 runtime/frontend/source/links 4줄. **emacs 버전을 동적으로 노출**해
  `daemon ≠ emacsclient` 면 `⚠ version skew` 를 화면에 띄운다 — 오늘 사건(초록불
  다 켜진 채 8 KB 넘는 응답만 조용히 썩음)이 사람 눈에 먼저 보이게 하는 장치다.
  WebTUI 버전은 하드코딩이 아니라 `<link>` href 에서 뽑아 CDN 과 어긋날 수 없다.
  변경: `emacs.clj`(`server-version`/`client-version`, coreutils `timeout` 로 묶음),
  `stats.clj`(`:versions`), `index.html`(CSS+마크업+JS).
  검증(재빌드 없이 가능한 범위): JS `node --check` 통과, 버전 추출 정규식 4상태
  (정상/스큐/데몬down/stats실패) 렌더 확인, 컨테이너에서 `timeout 5 emacsclient` 실동작 exit 0.
  **`index.html` 은 native-image 바이너리에 임베드된다**(jar `public/index.html` →
  바이너리 `strings` 에 CDN 문자열 존재). 컨테이너의 `resources/public` 볼륨 마운트는
  `io/resource` 경로에선 **읽히지 않는다** — 배포는 `./run.sh build --force` + recreate.
- **데몬 수명 소유권이 이 리포로 왔다 (GLG 결정 2026-09-02).** systemd 유닛은
  `disabled`, 정본은 `ops/tmux/agent-emacs.sh`(전용 tmux 세션 `agent-emacs`,
  `--fg-daemon`이라 죽으면 창에 남는다). **미결 한 건**: 현재 데몬(PID 845830)이
  아직 GLG 작업창 `doomemacs-config:1.1`에 얹혀 있다 — 그 창을 닫으면 죽는다.
  `./ops/tmux/agent-emacs.sh restart` 한 줄로 전용 세션에 옮긴다(다운타임 ~10초,
  GLG가 허용함). 부수 효과로 데몬의 `native-comp-eln-load-path`가 30.2를 가리키던
  문제도 해소된다(기동 시각이 31.1 eln 디렉토리 생성보다 1초 빨랐다).
  `--daemon=pi`(845920)가 같은 창에 있어 함께 죽는다 — pi 하네스 소관, GLG 판단.
- 안정화 단계에 꺼낼 것(지금 하지 않음, 문안은 `ops/README.md`에 확정):
  ① healthcheck `(+ 1 1)` → `(make-string 20000 ?x)` 왕복(autoheal이 없어져 이제 안전),
  ② `eval-elisp` bounded timeout + 무결성 가드(U+FFFD / `*ERROR*` / `&_&_`),
  ③ `/api/health`(②가 먼저), ④ emacsclient 해시 추적 종료(bridge는 nixos-config 쪽).
- Read: `ROADMAP.md`(북극성·축), `AGENTS.md`(검증·운영), botlog `20260331T123550`
  (논문 워크로그 SSOT), `docs/paper/geworfen.org`·`geworfen-ko.org`(논문 본체)
- Do not touch: nixos-config 안 건드림. systemd 유닛 SSOT는 `ops/systemd/`,
  설치 위치(`~/.config/systemd/user/`)는 거기서 동기화만.
  (예외 1회: 2026-09-01 라이브에 선반영된 `Type=notify`를 2026-09-02에 리포로
  **회수**했다. 라이브가 앞서고 SSOT가 뒤따른 의도된 미결이었고 — 근거는
  nixos-config `NEXT.md:246` — 회수로 닫혔다. 규칙은 그대로: 평상시 방향은 리포 → 설치.)

# RECENT
- [2026-09-02] **emacs 31 업그레이드 프리플라이트 — 이 리포 몫의 사실 정리.**
  형제 세션(nixos-config)이 오라클 격리 프로브로 버전 스큐를 실측해 보냈고,
  여기서 세 가지를 대조·정정했다. (1) geworfen이 부르는 elisp는 `agenda-day`
  **하나뿐**(`agenda.clj:140`) — `agenda-week`·`alive?`는 caller 없음. 그래서
  "week만 깨진다"가 아니라 **day가 이미 8 KB 경계를 넘는다**(실측).
  (2) compose 헤더의 "데몬 재시작 → 소켓 dir inode 재생성"은 upstream
  `server.el:665-679`(`/tmp/` 가드, bug#44644)으로 반증 — 진짜 경로는 런타임
  디렉토리 재생성(로그아웃/부팅 경합). 4/24 1차 사고는 inode가 아니라 소켓
  **이름** 변경이었다(`1b5270f`). (3) compose가 마운트한 emacs store path는
  시스템 프로파일이 참조하는 세대가 **아니다**(`s2j8ix…` vs `3a0zzkx…`) —
  주석이 거짓, GC 시한폭탄이 26.05 때와 같은 형태로 재발해 있다.
  수선: `ops/README.md` 2절 추가, `ARCHITECTURE.md` API 표 정정,
  `src/geworfen/emacs.clj` 미사용/미보호 지점 주석. **런타임·컨테이너 무접촉.**
- [2026-07-07] **논문 산출 파이프 이식 + 날것의 목소리 분업** (8442ab8). jacobian-lens/
  memex-kb 형제와 궁합: `build.el` = memex-kb `paper_build.el` 벤더링 사본(broken-links
  2줄 동기화), 변환 SSOT=memex-kb, 검증 baseline=jacobian-lens. `docs/paper/geworfen.*`
  패밀리(org SSOT → `make pdf`/`make html`; import 아닌 저자 downstream만), 생성물
  gitignore. 분업 하드룰 `AGENTS.md`. 봇로그 `20260331T123550` 방향 헤딩 추가.
- [2026-07-01] **데몬 hang 인시던트 + 뿌리 수정 3건**. (1) org-agenda가 리네임된
  botlog 파일에 `[R]emove/[A]bort?` 프롬프트 → 헤드리스 데몬 영구 hang → agenda
  다운. host `doomemacs-config/bin/agent-server.el`에 `org-agenda-skip-unavailable-files t`
  (17c27cb). (2) 데몬 PATH에 git 없어 denote git 조용히 실패 → `agent-emacs.service`
  `Environment=PATH`(db33446). (3) 형제 세션(nixos-config)이 감지·핸드오프, 복구 후
  회신. **셋 다 systemd `--user` 최소 PATH가 공통 뿌리**(timeout·git 부재).
  진단·복구 절차는 `ops/README.md`.
- [2026-07-01] **ROADMAP.md 신설** — arXiv 논문을 북극성으로, 기술축(관측성/
  시간축/데이터축)을 "논문 증거 생성기"로 정렬. `docs/paper/geworfen.org`가 이미 8~10쪽 ACM
  초안임을 재확인. Perplexity 논문 대조로 human-side expansion 프레임.
- [2026-07-01] **v2026.7.1** — 6/18 "자가복구"가 자해 루프였음 규명(watchdog ping이
  `timeout` 부재로 16,354회 재시작). watchdog 제거 + `Restart=no`. webtui 버전 고정.
  `AGENTS.md`에 SPA 검증법 기록.

# LEDGER
- geworfen은 org 데이터를 호스트 emacs 데몬에서 emacsclient로 읽는다.
  데몬 가용성 = geworfen 가용성. 운영 상세는 `ops/README.md`.
- 데몬은 이제 죽는 것(dead)이 아니라 **살아서 멈추는 것(hang)**이 주 위험.
  진단: `timeout 8 emacsclient -s server --eval '(+ 1 1)'` → exit 124면 hang,
  `journalctl --user -u agent-emacs.service`에서 프롬프트 확인. `Restart=no`라
  자동 부활 없음 — 사람/에이전트가 원인 보고 수동 기동.
- 페이지 검증은 `curl /`이 아니라 `/api/agenda`·`/api/stats` 직접 조회(SPA).
  상세는 `AGENTS.md`.
- geworfen = 존재 데이터 공개 표면 = 논문(arXiv)의 살아있는 증거면. 북극성 `ROADMAP.md`.
- **두 트랙 (헷갈리면 여기부터).** 트랙1=문턱·축적·측정 가능 = **지금 하는 것**.
  트랙2=만남·1KB·**측정하지 않음**(`geworfen#2`에 목소리로만). 비대칭 잠금 — 트랙1의
  결과는 트랙2를 증명도 반증도 못 한다. 트랙2에 "어떻게 검증하죠"를 묻지 마라.
  좌표는 `ROADMAP.md`「중심」, 원본은 denote `20250326T151413`.
- **이웃 리포 경계(하드룰, `AGENTS.md`)**: geworfen = 힣의 목소리(리서치 조사·레퍼런스
  사냥·연구자 톤 금지). jacobian-lens = 리서치 서포터. 산업계 서베이 *"기계의 마음을
  읽다 — 프런티어 랩의 내부·자아·정렬 연구 지형"*(6장; thesis = "안쪽만 파는 지형에
  바깥쪽 exoself 관점이 빈다")를 형제가 `jacobian-lens/survey/`에 작성 중(HTML/PDF
  산출까지, 오라클 서버 작업). geworfen은 **리뷰·소비**만(산업계 배경/인용원) —
  연구 노동 안 함. ROADMAP 대조 좌표의 산업계 축이 이걸로 두꺼워진다. **jacobian은
  형제 작업 중이라 건드리지 않는다.**
