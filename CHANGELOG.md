# Changelog

All notable changes to **geworfen** are recorded here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/).

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

[0.3.1]: https://github.com/junghan0611/geworfen/releases/tag/v0.3.1
[0.3.0]: https://github.com/junghan0611/geworfen/releases/tag/v0.3.0
[0.2.0]: https://github.com/junghan0611/geworfen/releases/tag/v0.2.0
