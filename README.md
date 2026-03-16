# geworfen

> **thrown into the world** — a WebTUI viewer that renders one human's raw existence data, unprocessed and transparent.

*The thrower of the project is thrown in his own throw.*  
*— Heidegger*

## What Is This

A real-time web dashboard for `www.junghanacs.com` — not a static blog, but a transparent data nexus of one human's daily life, co-lived with AI agents.

The front door is an org-agenda timeline. Behind it: existence data and agents, alive on the time axis.

## What It Shows

| Data | Scale | Format |
|------|-------|--------|
| Denote notes | 3,000+ | .org |
| Sleep / heart rate / time-tracking | 4,214+ | SQLite |
| Bibliography | 7,000+ | .bib |
| Daily journal | 696+ days | .org |
| Git commits | 14,000+ | git |
| Digital garden | 1,400+ | .md |

## Architecture

```
[Frontend]                    [Backend]                   [Data]
WebTUI CSS (CDN)              Clojure server              Emacs daemon
+ SF terminal aesthetics      (Ring / http-kit)            emacsclient
+ ASCII box org-agenda        GET  /api/agenda             ~/org/
+ SSE real-time updates       GET  /api/events (SSE)       lifetract.db
                              POST /api/trigger            .bib
```

Visitors hit a web page → `fetch /api/agenda` → Clojure calls `emacsclient` once → cached.  
Agent stamps → `curl /api/trigger` → SSE broadcast → all visitors update.

## Design — SF Terminal Aesthetics

**Not retro.** TRON. Blade Runner. A future terminal.

- Colors: neon green `#50fa7b`, cyan `#8be9fd`, deep dark `#0a0a1a`
- Fonts: Geist Pixel (headings) + Geist Mono + Pretendard (body)
- Layout: `ch`/`lh` character-based grid — org-agenda native

> *"A sufficiently advanced information ecosystem takes the form of the cleanest text terminal."*

## Development

```bash
nix develop          # enter dev environment
clj -M:dev           # start nREPL (CIDER)
clj -M:run           # start production server
bb dev               # dev server via Babashka
```

## Name

**geworfen** [ɡəˈvɔʁfn̩] — German for *"thrown"*

From Heidegger's *Geworfenheit* (thrownness): the fact that human existence is always already thrown into a world it did not choose. Every timestamp stamped by an agent into org-agenda is a facticity (*Faktizität*) — thrown into the world, raw and unprocessed. That's this project.

| Term | Meaning |
|------|---------|
| Geworfenheit | thrownness — the condition of being thrown |
| geworfen | thrown — past participle |
| werfen | to throw — infinitive |
| Dasein | being-there — human existence |
| Faktizität | facticity — the already-so |

## Links

- 📚 [Digital Garden](https://notes.junghanacs.com)
- 🐙 [GitHub](https://github.com/junghanacs)
- 🧵 [Threads](https://www.threads.net/@junghanacs)

## License

MIT
