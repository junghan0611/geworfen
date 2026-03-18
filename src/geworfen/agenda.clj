(ns geworfen.agenda
  "Agenda data layer — caches emacsclient calls, parses entries.
   100 visitors hitting the same date = 1 emacsclient call."
  (:require [geworfen.emacs :as emacs]
            [clojure.string :as str])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]
           [java.util.concurrent ConcurrentHashMap]))

;; ---------------------------------------------------------------------------
;; Cache — per-date, TTL-based
;; ---------------------------------------------------------------------------

(def ^ConcurrentHashMap cache (ConcurrentHashMap.))
(def ^:private cache-ttl-ms
  {:today    (* 30 1000)       ;; today: 30 seconds (stamps arrive frequently)
   :past     (* 60 60 1000)})  ;; past dates: 1 hour (data doesn't change)

(defn- cache-key ^String [date-str] (str "agenda:" date-str))

(defn- cache-get [date-str]
  (when-let [entry (.get cache (cache-key date-str))]
    (let [{:keys [data timestamp]} entry
          today? (= date-str (str (LocalDate/now)))
          ttl (if today? (:today cache-ttl-ms) (:past cache-ttl-ms))]
      (when (< (- (System/currentTimeMillis) ^long timestamp) ttl)
        data))))

(defn- cache-put! [date-str data]
  (.put cache (cache-key date-str)
        {:data data :timestamp (System/currentTimeMillis)}))

(defn invalidate-today!
  "Called when an agent stamps — clears today's cache so next request is fresh."
  []
  (.remove cache (cache-key (str (LocalDate/now)))))

;; ---------------------------------------------------------------------------
;; Parse agenda text into structured data
;; ---------------------------------------------------------------------------

(defn- parse-header [line]
  (when-let [[_ week] (re-find #"Day-agenda \(W(\d+)\):" line)]
    {:week (parse-long week)}))

(defn- parse-date-line [line]
  (when-let [[_ dow day month year] (re-find #"(\w+)\s+(\d+)\s+(\w+)\s+(\d{4})" line)]
    {:day-of-week dow :day (parse-long day) :month month :year (parse-long year)}))

(defn- parse-tags [text]
  "Extract tags from end of text: 'some text :tag1:tag2:' → [body tags-vec]"
  (let [[_ body tags-str] (re-find #"^(.*?)\s*(:[a-zA-Z0-9:]+:)\s*$" text)]
    (if tags-str
      [(or body text) (filterv (complement str/blank?) (str/split tags-str #":"))]
      [text []])))

(defn- parse-entry [line]
  (let [;; Category entry: "  Agent(T):  12:19...... text :tags:"
        ;;                  "  Human:     5:53...... text"
        ;;                  "  Schedule:  5:00-5:30  text"
        cat-ts-match (re-find #"^\s+([\w()]+):\s+(\d{1,2}:\d{2})\.\.\.\.\.\.\s+(.+)" line)
        cat-sched-match (re-find #"^\s+([\w()]+):\s+(\d{1,2}:\d{2})-(\d{1,2}:\d{2})\s+(.+)" line)
        ;; Time separator: "              10:00...... ----------------"
        sep-match (re-find #"^\s+(\d{1,2}:\d{2})\.\.\.\.\.\.\s+-{4,}" line)
        ;; Now line: "              14:01...... now - - -"
        now-match (re-find #"^\s+(\d{1,2}:\d{2})\.\.\.\.\.\.\s+now\s+-" line)
        ;; Bare timestamp (no category): "    5:53...... text"
        bare-match (re-find #"^\s+(\d{1,2}:\d{2})\.\.\.\.\.\.\s+(.+)" line)
        ;; Bare scheduled: "    5:00-5:30  text"
        bare-sched (re-find #"^\s+(\d{1,2}:\d{2})-(\d{1,2}:\d{2})\s+(.+)" line)]
    (cond
      now-match
      {:type "now" :time (second now-match)}

      sep-match
      {:type "separator" :time (second sep-match)}

      ;; Category + timestamp entry
      cat-ts-match
      (let [[_ source time text] cat-ts-match
            [body tags] (parse-tags text)]
        {:type "entry" :source source :time time :text body :tags tags})

      ;; Category + scheduled range
      cat-sched-match
      (let [[_ source start end text] cat-sched-match
            [body tags] (parse-tags text)]
        {:type "entry" :source source :time start
         :text (str body " (" start "-" end ")") :tags tags})

      ;; Bare timestamp (fallback, no category)
      bare-match
      (let [[_ time text] bare-match
            [body tags] (parse-tags text)]
        {:type "entry" :source "Human" :time time :text body :tags tags})

      ;; Bare scheduled range (fallback)
      bare-sched
      (let [[_ start end text] bare-sched
            [body tags] (parse-tags text)]
        {:type "entry" :source "Schedule" :time start
         :text (str body " (" start "-" end ")") :tags tags})

      (not (str/blank? line))
      {:type "raw" :text (str/trim line)})))

(defn parse-agenda
  "Parse org-agenda day text into structured data."
  [text]
  (let [lines (str/split-lines text)
        header (some parse-header lines)
        date-info (some parse-date-line lines)
        entries (->> lines
                     (drop 2)  ;; skip header + date line
                     (keep parse-entry)
                     (vec))]
    (merge header date-info {:entries entries})))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn- within-range?
  "Only serve dates within 14 days of today."
  [date-str]
  (try
    (let [date (LocalDate/parse date-str)
          today (LocalDate/now)
          min-date (.minusDays today 14)
          max-date (.plusDays today 1)]
      (and (not (.isBefore date min-date))
           (not (.isAfter date max-date))))
    (catch Exception _ false)))

(defn get-day
  "Get parsed agenda for a date. Uses cache. Limited to ±14 days."
  [date-str]
  (if (within-range? date-str)
    (or (cache-get date-str)
        (let [raw (emacs/agenda-day date-str)
              parsed (assoc (parse-agenda raw) :raw raw)]
          (cache-put! date-str parsed)
          parsed))
    {:error "out of range" :message "Only the last 14 days are available."}))

(defn get-today
  "Get today's agenda."
  []
  (get-day (str (LocalDate/now))))

(defn date->str
  "Offset string to ISO date string for API responses."
  [offset-or-date]
  (cond
    (re-matches #"\d{4}-\d{2}-\d{2}" offset-or-date)
    offset-or-date

    (re-matches #"[+-]?\d+" offset-or-date)
    (str (.plusDays (LocalDate/now) (parse-long offset-or-date)))

    :else
    (str (LocalDate/now))))
