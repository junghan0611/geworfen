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
- Blocker: none
- Read: `ROADMAP.md`(북극성·축), `AGENTS.md`(검증·운영), botlog `20260331T123550`
  (논문 워크로그 SSOT), `docs/paper/geworfen.org`·`geworfen-ko.org`(논문 본체)
- Do not touch: nixos-config 안 건드림. systemd 유닛 SSOT는 `ops/systemd/`,
  설치 위치(`~/.config/systemd/user/`)는 거기서 동기화만.

# RECENT
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
- **이웃 리포 경계(하드룰, `AGENTS.md`)**: geworfen = 힣의 목소리(리서치 조사·레퍼런스
  사냥·연구자 톤 금지). jacobian-lens = 리서치 서포터. 산업계 서베이 *"기계의 마음을
  읽다 — 프런티어 랩의 내부·자아·정렬 연구 지형"*(6장; thesis = "안쪽만 파는 지형에
  바깥쪽 exoself 관점이 빈다")를 형제가 `jacobian-lens/survey/`에 작성 중(HTML/PDF
  산출까지, 오라클 서버 작업). geworfen은 **리뷰·소비**만(산업계 배경/인용원) —
  연구 노동 안 함. ROADMAP 대조 좌표의 산업계 축이 이걸로 두꺼워진다. **jacobian은
  형제 작업 중이라 건드리지 않는다.**
