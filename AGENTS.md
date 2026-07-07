# AGENTS.md — geworfen

에이전트가 이 repo에서 일할 때의 baseline. 세션마다 바뀌는 다음 할 일은
`NEXT.md`에.

## 무엇인가

`agenda.junghanacs.com`을 서빙하는 Clojure HTTP 서버 (GraalVM native-image로
컴파일된 단일 바이너리, Docker 안에서 실행). org-agenda 데이터는 **호스트의
emacs `server` 데몬**에서 `emacsclient`로 가져온다. 프론트엔드는
`resources/public/index.html` 한 장짜리 SPA (WebTUI + Catppuccin).

## 페이지 검증 — `curl`로 판단하지 마라 (중요)

`/`는 **클라이언트 JS가 `fetch('/api/agenda')`로 엔트리를 채우는 SPA**다.
`curl https://agenda.junghanacs.com/` 은 JS를 실행하지 않으므로 엔트리가
비어 보인다 — 이걸 "엔트리 사라짐"으로 오판하기 쉽다. 실제로 2026-07-01에
이 함정으로 정상 사이트를 장애로 착각했다.

올바른 검증:

```bash
# 데이터는 API를 직접 때려서 본다 (JS 없이도 진실)
curl -s 'https://agenda.junghanacs.com/api/agenda?date=2026-07-01' | jq '.entries | length'
curl -s  https://agenda.junghanacs.com/api/stats | jq .

# 날짜 포맷 주의: 페이지 텍스트는 ISO가 아니라 "1 July 2026", "9:47".
#   2026-07-01 같은 ISO 정규식으로 grep하면 있는 엔트리도 못 찾는다.
# 초기 HTML엔 noscript SSR로 '오늘' 엔트리가 인라인돼 있다 (deep-link/미리보기용).
```

렌더 결과(레이아웃/색상)까지 봐야 하면 `curl`이 아니라 browser-tools 스킬로
실제 브라우저를 띄운다.

## emacs 데몬 = geworfen 가용성

데몬이 죽으면 모든 페이지가 HTTP 500. 운영은 `ops/README.md`가 SSOT.

- `agent-emacs.service`는 **`Restart=no`** — 죽으면 자동 부활 안 한다.
  의도적이다: 죽는 신호가 즉시 드러나야 하고(핵심 개인 서버), 자동 재시작은
  근본 원인을 숨긴다. 목표는 "점점 더 안 죽는 것".
- 죽어 있으면 원인(`journalctl --user -u agent-emacs.service`)을 먼저 보고
  나서 `systemctl --user start agent-emacs.service`로 사람/에이전트가 켠다.
- systemd 유닛 SSOT는 `ops/systemd/`. 설치 위치(`~/.config/systemd/user/`)는
  거기서 동기화만. **nixos-config는 건드리지 않는다.**

## 프론트엔드 의존성

`index.html`의 CDN(WebTUI 등)은 **버전을 고정**한다 — `@latest` 금지.
재현성 때문이고, `@webtui/theme-catppuccin`은 오발행 버전(`26.2.0` 등)이
섞여 있어 `@latest`가 특히 위험하다. 올릴 때는 npm registry에서 실제 최신을
확인하고, 고정 URL이 HTTP 200으로 살아있는지 검증한 뒤 커밋한다.

```bash
curl -s https://registry.npmjs.org/@webtui/css | jq -r '."dist-tags".latest'
```

## 빌드 / 릴리즈

- Dev: `nix develop -c bash` → `clj -M:run` (포트 3333)
- Native: `./run.sh build` → `./run.sh serve`
- 버전은 **CalVer** `vYYYY.M.D` (시간축 스냅샷). SemVer 아님. `tag-release`
  스킬을 따른다.

## 논문 산출 파이프 (`docs/paper/`) — 이웃 리포

geworfen은 힣 연구 논문의 **정본**이다. `docs/paper/geworfen-ko.org`(한글 씨앗)
→ `docs/paper/geworfen.org`(영문 acmart SSOT) → 산출 패밀리(`geworfen.pdf`/
`.tex`/`.bbl` · `geworfen.html` · 나중에 `geworfen.interactive.org`+`capsule/`).
논문 워크로그 SSOT는 botlog `20260331T123550`, 리포 중심 정렬은 `ROADMAP.md`.
산출 명령은 `docs/paper/Makefile` (`make pdf` / `make html`).

- **변환 로직 SSOT = memex-kb** (`paper2org-*`, `paper_build.el`). 우리는
  저자라 memex-kb의 URL import 단계는 안 쓰고 **downstream transform만** 쓴다
  (org→PDF=`build.el`, org→html5=pandoc `--citeproc --mathjax`). `build.el`은
  memex-kb 조상을 **벤더링한 self-contained 사본** — geworfen은 외부 리포 없이
  혼자 빌드돼야 하므로(재현성 원칙) 런타임 의존 대신 사본을 두고, 레시피가
  개선되면 memex-kb → 여기로 동기화(예: broken-links/debug 2줄).
- **산출 로직 검증 baseline = jacobian-lens** (`PAPER-IMPORT.md`). 앤트로픽
  J-space 논문으로 org→acmart PDF / org→html5 citeproc / 오프라인 캡슐 전
  파이프가 동작함이 거기서 보증됨.

### 역할 분업 — geworfen은 날것의 목소리를 지킨다 (하드 룰)

힣이 못박은 원칙(2026-07-07). 이 리포에서 일하는 에이전트는 지킨다:

- **geworfen = 힣의 목소리**. 힣 데이터로 힣이 하고 싶은 말을 쓴다.
  **리서치 조사 안 한다. 레퍼런스 사냥 안 한다. 연구자 톤으로 말하지 않는다.**
  대학원식 "이 레퍼런스 맞냐 아니냐" 뒤지기는 여기서 하지 않는다. geworfen
  에이전트가 연구자처럼 굴면 "또 똑같은 논문"이 나오고 힣의 독창성(1KB·존재
  대 존재·autobiographical)이 죽는다. 이게 실패 모드다.
- **jacobian-lens = 리서치 서포터**. 남의 연구(앤트로픽 등)를 많이 품어서
  레퍼런스 조사·검증이라는 "연구자 노동"을 대신하는 창고. 거기 뭘 쌓아도
  앤트로픽 것이지 힣 것이 아니다. 연구 노동이 필요하면 그쪽으로 넘긴다.
- 경계: **연구 노동 → jacobian-lens, 날것의 목소리 → geworfen.** 여기서
  포맷·파이프·데이터 시각화(인프라)는 돕되, 논문의 목소리·주장·해석은
  힣의 것을 힣의 데이터로 담는다.
