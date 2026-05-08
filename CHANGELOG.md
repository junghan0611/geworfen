# Changelog

All notable changes to **geworfen** are recorded here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/).

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

First public milestone — minimalist UI, native binary, live deploy.

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
- GraalVM native-image build — 43 MB single binary, instant startup.
- Per-date TTL cache so 100 visitors on the same date = 1
  `emacsclient` call.

[0.3.0]: https://github.com/junghan0611/geworfen/releases/tag/v0.3.0
[0.2.0]: https://github.com/junghan0611/geworfen/releases/tag/v0.2.0
