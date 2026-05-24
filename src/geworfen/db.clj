(ns geworfen.db
  "Read-only access to lifetract.db for existence-data stats.
   Path can be overridden via LIFETRACT_DB env. Returns nil on any failure
   so /api/stats stays usable when the DB is absent (e.g. unmounted in container)."
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]))

(defn- db-path []
  (or (System/getenv "LIFETRACT_DB")
      (str (System/getProperty "user.home")
           "/repos/gh/self-tracking-data/lifetract.db")))

(defn- spec []
  ;; SQLite URI filename syntax (`file:` prefix) so query parameters are honored.
  ;; immutable=1: WAL/shm 무시 + locking 비활성. read-only 마운트에서 안전.
  ;; 매 호출마다 새 connection → file 다시 열리므로 lifetract 갱신은 자연 반영.
  {:jdbcUrl (str "jdbc:sqlite:file:" (db-path) "?immutable=1")})

(defn sleep-days
  "Distinct calendar days with at least one sleep row.
   nil if DB missing or query fails."
  []
  (when (.exists (io/file (db-path)))
    (try
      (-> (jdbc/execute-one!
            (spec)
            ["SELECT COUNT(DISTINCT date(start_time)) AS n FROM sleep"])
          :n)
      (catch Exception _ nil))))
