# geworfen — Architecture: Emacs ↔ Clojure Interface

## Data Sources

org-agenda reads from **3 files** (configured in `org-agenda-files`):

```
~/org/meta/20230202T020200--now__aprj_meta.org       # inbox/meta (human)
~/org/botlog/agenda/*__agenda_thinkpad.org            # agent stamps (thinkpad)
~/org/botlog/agenda/*__agenda_oracle.org              # agent stamps (oracle)
```

There is also `diary.org` (18,951 lines, 2022~2025 human entries) which is
NOT currently in org-agenda-files but could be added.

## Agenda File Structure — Reverse Datetree

```org
** 2026-03 March
*** 2026-03-17 Tuesday
**** entry text here :tag1:tag2:
<2026-03-17 Tue 13:55>
**** another entry :commit:pi:
<2026-03-17 Tue 14:20>
*** 2026-03-16 Monday
**** older entry :botlog:
<2026-03-16 Mon 09:30>
```

- Level 2: `** YYYY-MM Month`
- Level 3: `*** YYYY-MM-DD Weekday`
- Level 4: `**** text :tags:`
- Followed by `<timestamp>` on next line
- Newest entries at top (reverse datetree)
- Optional `:LOGBOOK:` / `CLOCK:` blocks

## Performance: emacsclient Call Timing

```
today        →  50ms
yesterday    → 210ms
1 month ago  →  10ms
```

emacsclient = Unix socket IPC. No network overhead.
org-agenda build = Emacs internal scanning (~ms level for 3 files).

## The Core Question: Who Parses?

### Option A: emacsclient per request (current)

```
Browser → Clojure → emacsclient → Emacs org-agenda → text → Clojure → JSON → Browser
```

| Pro | Con |
|-----|-----|
| Same view as Emacs users | Emacs is single-threaded |
| No reimplementation | Serialized under load |
| org-agenda handles all edge cases | Emacs daemon dependency |
| 50ms per call | |

With caching (30s today / 1h past): 100 visitors = effectively 1-2 emacsclient calls/minute.

**Risk**: If 10 users request 10 different uncached dates simultaneously,
Emacs processes them sequentially: 10 × 50ms = 500ms total. Acceptable.

### Option B: Clojure parses org files directly

```
Browser → Clojure → read agenda files → parse reverse datetree → JSON → Browser
```

| Pro | Con |
|-----|-----|
| Multi-threaded, concurrent | Reimplements org-agenda in Clojure |
| No Emacs dependency | Must handle org syntax edge cases |
| Can watch files for changes | Drift from "what Emacs shows" |

The reverse datetree is simple enough to parse:
- Scan level-3 headings for date match
- Collect level-4 entries under that date
- Extract timestamps and tags

But we lose: time sorting across files, category prefixes, time grid,
scheduled/deadline handling, and any future org-agenda features.

### Option C: Hybrid — Emacs pre-generates, Clojure serves

```
On trigger/interval:
  Emacs → generate agenda for today → write JSON to /tmp/geworfen-cache/
  Emacs → generate agenda for recent 7 days → write JSON

On request:
  Browser → Clojure → read cached JSON → respond
  If cache miss → emacsclient one-shot → cache → respond
```

| Pro | Con |
|-----|-----|
| Emacs does the hard work once | More moving parts |
| Clojure serves instantly | Trigger mechanism needed |
| Best of both worlds | Stale data window |

### Option D: Clojure + emacsclient fallback

```
For recent dates (in agenda files):
  Clojure parses org files directly (fast, concurrent)

For complex queries (tags, search, cross-file):
  Falls back to emacsclient
```

| Pro | Con |
|-----|-----|
| Fast for common case | Two code paths to maintain |
| Emacs for hard cases | Parsing parity risk |

## Recommendation

**Start with Option A** (current). The numbers prove it works:

- 50ms per emacsclient call
- Cache absorbs 99% of traffic
- 100 concurrent users hitting different dates = worst case 5 seconds total serialized Emacs time
- Single-threaded Emacs is only a problem at >1000 concurrent uncached requests

**Monitor and evolve toward Option C or D** when:
- Emacs daemon reliability becomes an issue
- We need sub-10ms response times
- We add features beyond day-view (search, tag filter, graph)

The key insight: **the agenda files are simple enough for Clojure to parse**,
so we always have an escape hatch. But right now, Emacs-way is correct —
same function, same view, same data.

## Emacs API Surface (agent-server)

Functions used by geworfen:

| Function | Input | Output | Use |
|----------|-------|--------|-----|
| `agent-org-agenda-day` | date string or offset | formatted text | Day timeline |
| `agent-org-agenda-week` | date string or offset | formatted text | Week view |
| `agent-org-agenda-tags` | tag match string | formatted text | Tag filter |

Future needs:
- Date range export (batch)
- Structured output (s-expression or JSON instead of text)
- File change notification hook

## File Locations

```
~/org/botlog/agenda/       # agent stamp files (org-agenda source)
~/org/meta/                # human meta/inbox (org-agenda source)
~/org/diary.org            # historical human entries (not in agenda currently)
~/org/journal/             # weekly journals (not in agenda)
~/org/notes/               # denote notes (not in agenda)
```
