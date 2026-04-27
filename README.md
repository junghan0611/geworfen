# geworfen

> **thrown into the world** — a WebTUI viewer that renders one human's raw existence data, unprocessed and transparent.

*The thrower of the project is thrown in his own throw.*
*— Heidegger*

![geworfen — org-agenda live on agenda.junghanacs.com](docs/screenshot.png)

*Live at [agenda.junghanacs.com](https://agenda.junghanacs.com) — 42MB native binary, Emacs org-agenda served via Docker + agent-server, WebTUI Catppuccin theme, GLG-Mono font.*

## What Is This

A real-time web dashboard — not a static blog, but a transparent data nexus of one human's daily life, co-lived with AI agents.

The front door is an org-agenda timeline. Behind it: existence data and agents, alive on the time axis. The same `agent-org-agenda-day` function that Emacs users see, that bots see — this page calls it too.

### v0.2 — Minimalism

Two panels, no chrome. Human journal + Agent stamps + Diary schedules on a single time axis. Commit links are clickable — each entry is a door to the code that made it. The design principle: **show the data, hide the interface.**

## Architecture

```
[Browser]                     [Docker Container]           [Host]
WebTUI + Catppuccin           geworfen binary              Emacs daemon
GLG-Mono font                 (GraalVM native, 43MB)       agent-server.el
fetch /api/agenda?date=  →    Clojure server          →    emacsclient
                              http-kit + reitit             ~/org/ (agenda files)
                              per-date cache (30s/1h)
```

- 100 visitors hitting the same date = **1 emacsclient call** (cached)
- 10 visitors on 10 different dates = 10 × 50ms = 500ms serialized
- Native binary: **instant startup**, ~30MB RAM, no JVM needed

## Existence Data

| Data | Scale | Format |
|------|-------|--------|
| Denote notes | 3,300+ | .org |
| Bibliography | 8,200+ | .bib |
| Git commits | 8,500+ | git |
| Daily journal | 1,477+ days | .org |
| Health records | 4,400+ | SQLite |
| Digital garden | 2,100+ | .md |

## Build & Run

```bash
# Development (JVM)
nix develop -c bash
clj -M:run                    # server on port 3333

# Production (native binary)
nix develop -c bash
./run.sh build                # GraalVM native-image (~31s)
./run.sh serve                # run binary (instant startup)

# Docker (recommended for deployment)
# geworfen binary inside container,
# connects to host Emacs via emacsclient socket mount
```

## API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | WebTUI org-agenda viewer |
| `/api/agenda?date=2026-03-17` | GET | Parsed agenda + raw text (JSON) |
| `/api/stats` | GET | Existence data counts (JSON) |
| `/api/trigger` | POST | Invalidate today's cache |

## Keyboard Navigation

| Key | Action |
|-----|--------|
| `←` / `→` | Previous / next day |
| `.` | Jump to today |

## Name

**geworfen** [ɡəˈvɔʁfn̩] — German for *"thrown"*

From Heidegger's *Geworfenheit* (thrownness): the fact that human existence is always already thrown into a world it did not choose. Every timestamp stamped by an agent into org-agenda is a facticity (*Faktizität*) — thrown into the world, raw and unprocessed. That's this project.

## Ecosystem

geworfen is part of a larger system — one human's reproducible knowledge and computing environment:

| Project | Description |
|---------|-------------|
| [geworfen](https://github.com/junghan0611/geworfen) | This project — existence data WebTUI viewer |
| [doomemacs-config](https://github.com/junghan0611/doomemacs-config) | Doom Emacs configuration — org-agenda, denote, agent-server.el |
| [nixos-config](https://github.com/junghan0611/nixos-config) | NixOS system configuration — reproducible across 4 machines |
| [agent-config](https://github.com/junghan0611/agent-config) | AI agent orchestration — 24 skills, semantic memory, multi-device |
| [GLG-Mono](https://github.com/junghan0611/GLG-Mono) | Korean programming font — the font this viewer uses |
| [notes](https://github.com/junghanacs/notes.junghanacs.com) | Digital garden — [notes.junghanacs.com](https://notes.junghanacs.com) |

## Links

- 📚 [Digital Garden](https://notes.junghanacs.com)
- 🐙 [GitHub @junghanacs](https://github.com/junghanacs)
- 🧵 [Threads](https://www.threads.net/@junghanacs)
- 🦋 [Bluesky](https://bsky.app/profile/junghanacs.bsky.social)
- 🐘 [Mastodon](https://fosstodon.org/@junghanacs)

## License

MIT
