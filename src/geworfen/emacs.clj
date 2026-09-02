(ns geworfen.emacs
  "Emacs agent-server bridge — calls emacsclient, returns parsed results.
   This is the Emacs-way: we call the same functions that humans and bots use."
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(def ^:private socket-name
  (or (System/getenv "GEWORFEN_EMACS_SOCKET") "server"))

(defn eval-elisp
  "Evaluate elisp via emacsclient connected to the agent emacs daemon.
   Socket name defaults to `server` and can be overridden via GEWORFEN_EMACS_SOCKET.

   Known gaps (2026-09-02, documented not fixed — see ops/README.md):
   1. No timeout. `clojure.java.shell/sh` blocks forever if the daemon hangs,
      pinning an http-kit worker thread. That is the 000 half of the
      \"전 페이지 500/000\" failure mode in ops/README.md.
   2. No integrity check. Only `exit` is inspected, so a corrupted-but-exit-0
      payload (emacsclient/server version skew, upstream Bug#80807) is cached
      and served as if fine.

      The skew also splits a 3-byte Hangul char at the recv boundary, producing
      INVALID UTF-8. That does NOT raise here: `sh` decodes via
      clojure.java.io/copy `do-copy [InputStream Writer]`, which builds an
      `InputStreamReader` (clojure 1.12.0 io.clj:313-320) whose contract is to
      REPLACE malformed input, and `:out-enc` defaults to \"UTF-8\"
      (shell.clj:47). So the bytes become U+FFFD silently — exit 0, empty err,
      valid JSON out, wrong content. (Python's strict decode raises on the same
      bytes; the JVM does not. Do not carry that expectation over.)

      Guard to add (all three verified zero-false-positive against live
      2026-08-25/08-30/09-01/09-02 payloads, 2026-09-02):
        U+FFFD  |  \"*ERROR*: Unknown message:\"  |  #\"&_&_\"
      Note the same InputStreamReader is why normal multi-byte output does NOT
      break at the 1024-char buffer boundary — the decoder carries partial
      sequences across reads. Only genuinely invalid bytes are replaced."
  [expr]
  (let [{:keys [out err exit]} (sh "emacsclient" "-s" socket-name "--eval" expr)]
    (if (zero? exit)
      ;; emacsclient wraps output in quotes and escapes newlines
      (-> out
          str/trim
          (str/replace #"^\"" "")
          (str/replace #"\"$" "")
          (str/replace "\\n" "\n")
          (str/replace "\\\"" "\""))
      (throw (ex-info "emacsclient failed" {:expr expr :err err :exit exit})))))

(defn agenda-day
  "Get org-agenda day view for a given date string.
   date-str: \"2026-03-17\", \"-1\" (yesterday), \"+3\" (3 days from now), or nil (today)"
  ([]
   (eval-elisp "(agent-org-agenda-day)"))
  ([date-str]
   (eval-elisp (format "(agent-org-agenda-day \"%s\")" date-str))))

(defn agenda-week
  "Get org-agenda week view.

   NOT ON ANY REQUEST PATH as of 2026-09-02 — no caller (`grep -rn \"emacs/\" src/`
   returns only agenda.clj:140). Kept as the wrapper for a future week view.
   Size note: this returns ~26 KB, day returns 2.5-8.8 KB."
  ([]
   (eval-elisp "(agent-org-agenda-week)"))
  ([date-str]
   (eval-elisp (format "(agent-org-agenda-week \"%s\")" date-str))))

;; ---------------------------------------------------------------------------
;; Version surface — the footer shows these so a skew is visible on the page
;; ---------------------------------------------------------------------------
;;
;; Why this exists: on 2026-09-02 an emacsclient/server version skew corrupted
;; agenda payloads over ~8 KB while every green light stayed green (exit 0,
;; HTTP 200, healthcheck pass). Nothing on the page said which Emacs was
;; answering. Publishing both versions makes that class of failure visible to a
;; human before it is visible in the data. See ops/README.md.
;;
;; Both calls are wrapped in coreutils `timeout` because `eval-elisp` has no
;; bound and a hung daemon would otherwise pin an http-kit worker thread — and
;; a down/hung daemon is exactly the state in which someone loads this page.
;; `timeout` is present in the container image (GNU coreutils 9.1 at
;; /usr/bin/timeout, verified 2026-09-02). If it were ever missing, `sh` returns
;; exit 127 and we yield nil — the footer shows "?" rather than lying. Do NOT
;; read a 127 here as "the daemon is down"; that conflation is what turned the
;; 2026-06 watchdog into a 16,354-restart self-harm loop (ops/README.md).

(def ^:private probe-timeout-s "5")

(defn- probe
  "Run emacsclient under a hard wall-clock bound. nil on any failure."
  [& args]
  (try
    (let [{:keys [out exit]} (apply sh "timeout" probe-timeout-s "emacsclient" args)]
      (when (zero? exit) (str/trim out)))
    (catch Exception _ nil)))

(defn server-version
  "Emacs version string reported by the daemon (e.g. \"31.1\"), or nil."
  []
  (some->> (probe "-s" socket-name "--eval" "(emacs-version)")
           (re-find #"GNU Emacs ([0-9][0-9.]*)")
           second))

(defn client-version
  "Version of the emacsclient binary we actually invoke (e.g. \"31.1\"), or nil.
   This is the container's binary, which is mounted from the host nix store and
   can drift from the daemon independently — that drift is the whole point."
  []
  (some->> (probe "--version")
           (re-find #"emacsclient ([0-9][0-9.]*)")
           second))

(defn alive?
  "Check if the agent emacs daemon is reachable.

   NOT CALLED — there is no /api/health route (server.clj:107-112). The live
   liveness probe is the compose healthcheck running the same `(+ 1 1)` ping
   from outside. That ping is 1 byte and therefore cannot detect the version-skew
   corruption described in ops/README.md; see ROADMAP.md 축 1 for /api/health."
  []
  (try
    (= "2" (str/trim (:out (sh "emacsclient" "-s" socket-name "--eval" "(+ 1 1)"))))
    (catch Exception _ false)))
