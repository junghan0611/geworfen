# Changelog

All notable changes to **geworfen** are recorded here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions are CalVer date snapshots — `vYYYY.M.D`, evolving along the time
axis. (Pre-2026.6 tags used SemVer `0.x`; kept as-is for history.)

## [2026.7.1] — 자가복구 회수 · webtui 버전 고정

6/18에 넣은 emacs 데몬 "자가복구"가 실은 자해 루프였다. watchdog의 ping
명령 `timeout 5 emacsclient ...` 가 systemd `--user` PATH에 `timeout`이 없어
매번 exit 127(command not found)로 떨어졌고, `|| restart` 가 발동해 **멀쩡한
데몬을 6/18~7/1 동안 16,354회** 죽였다. 진짜 다운은 없었고 로그만 오염됐다.
자동 복구는 "점점 더 안 죽는다"는 목표와 반대로 죽는 신호를 지운다고 보고,
자가복구를 통째로 걷어냈다.

### Removed
- `ops/systemd/agent-emacs-watchdog.service` / `.timer` — 자해 루프. 제거.

### Changed
- `agent-emacs.service` `Restart=always` → **`Restart=no`**. 데몬이 죽으면
  자동 부활 없이 그대로 두어 문제가 즉시 드러나게 한다(핵심 개인 서버).
  다시 켜기는 사람/에이전트 수동 (`systemctl --user start`).
- `resources/public/index.html` — WebTUI CDN을 `@latest`에서 **버전 고정**:
  `@webtui/css@0.1.9`, `@webtui/theme-catppuccin@0.0.5`. 재현성 확보 +
  `theme-catppuccin`의 오발행 버전(`26.2.0` 등)에 `@latest`가 끌려갈 위험 차단.
- `ops/README.md` / `README.md` — 자가복구 서술을 "재시작 안 함" 운영
  철학으로 정정.

### Added
- `AGENTS.md` — repo baseline. 특히 **페이지 검증**: `/` 는 JS가
  `fetch('/api/agenda')` 로 채우는 SPA라 `curl` 로는 엔트리가 비어 보인다.
  진실은 `/api/agenda`·`/api/stats` 를 직접 조회해야 한다(2026-07-01에 이
  함정으로 정상 사이트를 장애로 오판한 기록을 남김).

## [2026.6.18] — emacs 데몬 자가복구 인프라

호스트 emacs `server` 데몬 자가복구를 systemd user 서비스로 정착.
6/17 hang·6/18 dead로 사이트가 두 번 HTTP 500을 냈고, 데몬 가용성이
곧 geworfen 가용성임이 드러나 운영 인프라를 repo에 SSOT로 기록했다.

### Added
- `ops/systemd/` — emacs 데몬 자가복구 systemd user 유닛 3종:
  `agent-emacs.service` (`Restart=always`, **dead** 복구),
  `agent-emacs-watchdog.timer` (1분 ping, **hang** 복구),
  그리고 `loginctl enable-linger` (세션 무관 상주).
  SIGKILL 테스트로 6초 자동 복구 검증.
- `ops/README.md` — 데몬 의존 구조·설치·디버그(`journalctl --user -u
  agent-emacs`)·원인 분석 운영 노트.
- `NEXT.md` — 세션 핸드오프(NOW/RECENT/LEDGER).

### Changed
- `README.md` Architecture에 데몬 자가복구(`ops/README.md`) 링크 추가.

### Notes
- nixos-config 불변경. 유닛 SSOT는 `ops/systemd/`, 설치 위치
  `~/.config/systemd/user/`는 거기서 동기화만.
- cron `agent-server-healthcheck.sh`는 systemd로 역할이 이관되어 deprecated.

## [0.3.1] — 2026-05-24

`health` stat이 lifetract.db에서 실제 수면 일수를 읽기 시작했다.
placeholder 4489 → **2,573d** (2017-03-04 이래 sleep row가 있는 날 수).
새 sleep row가 들어오면 1시간 캐시 만료 후 자연 갱신.

### Added
- `geworfen.db` ns — xerial `sqlite-jdbc` 로 lifetract.db read-only
  접근. URI 문법 `jdbc:sqlite:file:...?immutable=1` 로 WAL/shm 무시
  + locking 비활성, ro 마운트에서 안전. 매 호출마다 새 connection
  을 열어 외부 갱신을 자연 반영.
- `LIFETRACT_DB` 환경변수로 DB 경로 override.

### Changed
- `/api/stats` 의 `health` 자리가 lifetract.db의
  `COUNT(DISTINCT date(start_time)) FROM sleep` 결과를 반환. DB가
  없거나 쿼리가 실패하면 `null`, frontend 는 해당 줄을 그리지 않음.
- `health` 표시 단위에 `d` suffix (journal 과 동일 단위축).

### Infra
- 페어 변경: `nixos-config` `docker/geworfen/docker-compose.yml` 에
  `~/repos/gh/self-tracking-data/lifetract.db` ro 마운트 한 줄 추가.

## [0.3.0] — 2026-05-08

Live at <https://agenda.junghanacs.com>.

### Added
- `?date=YYYY-MM-DD` URL parameter — open the page on any date within
  the ±14-day window. Honored by both the noscript SSR pre-render and
  the JS day navigator, so deep links are bookmarkable and crawlable.
- noscript Server-Side Rendering — today's agenda (or the date in the
  query) is inlined into the HTML, so the page is readable without
  JavaScript and search engines can index daily content.
- `og:description` meta-tag, dynamically built from the first agenda
  entries — link previews on Slack, Threads, etc. show the actual day.
- `/robots.txt` route.
- `ENTWURF` and `COS` agenda categories with their own colors
  (peach, teal). TODO/NEXT/DONE/DONT keywords and `[#A]`–`[#D]`
  priorities are now colorized inline in the agenda body.
- Auto-jump to today when the page is left open across midnight.
- Umami analytics tag.

### Changed
- Cache TTL split: today = 60s (syncthing-friendly), past dates = 1h.
- `journal` stat is now days-since-2022-03-10 (started daily, switched
  to weekly files; file count no longer matches lived days).
- README live screenshot — replaced the 775KB v0.1 mockup with a
  191KB live capture.
- Default Emacs socket name `agent-server` → `server` to match the
  shared agent daemon convention.
- Cleanup: removed the unused `.beads/` issue tracker.

### Fixed
- KST timezone in date formatting and the midnight rollover.
- `Cache-Control: no-cache` on the index response so browsers don't
  serve a stale shell after a deploy.
- Daemon agent agenda no longer shows DONT entries (paired with
  doomemacs-config `f998631`). Human Doom agenda is unaffected.

## [0.2.0] — 2026-03-17

First public milestone — minimalist UI, Clojure server shipped as a
GraalVM `native-image` binary (no JVM at runtime), live deploy.

### Added
- WebTUI org-agenda viewer — Catppuccin theme, GLG-Mono font.
- Two-panel layout: existence data + public key/links.
- Human / Agent / Diary categories on a single time axis.
- Clickable org-mode links for commit URLs.
- Keyboard navigation (`←` `→` for day, `.` for today).
- Out-of-range guard (±14 days).
- Mobile responsive layout.
- `/api/stats` — dynamic existence data counts.
- `/api/trigger` — invalidate today's cache after agent stamps.
- GraalVM `native-image` build of the Clojure server — 43 MB single
  binary, instant startup, no JVM at runtime. (The binary is only the
  geworfen HTTP server; Emacs runs separately on the host and is
  reached via `emacsclient`.)
- Per-date TTL cache so 100 visitors on the same date = 1
  `emacsclient` call.

[2026.7.1]: https://github.com/junghan0611/geworfen/releases/tag/v2026.7.1
[2026.6.18]: https://github.com/junghan0611/geworfen/releases/tag/v2026.6.18
[0.3.1]: https://github.com/junghan0611/geworfen/releases/tag/v0.3.1
[0.3.0]: https://github.com/junghan0611/geworfen/releases/tag/v0.3.0
[0.2.0]: https://github.com/junghan0611/geworfen/releases/tag/v0.2.0
