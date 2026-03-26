(ns geworfen.stats
  "Existence data stats — simple file counts."
  (:require [clojure.java.io :as io])
  (:import [java.time LocalDate]
           [java.time.temporal ChronoUnit]))

(defonce ^:private cache (atom nil))
(def ^:private ttl-ms (* 60 60 1000)) ;; 1 hour

;; 저널 시작일 — 2022-03-10 (첫 denote 저널 파일)
;; daily → weekly 전환하면서 파일 수 ≠ 기록 일수.
;; 파일 갯수 대신 시작일~오늘 일수를 동적으로 계산.
;; 일일일생(一日一生).
(def ^:private journal-start (LocalDate/of 2022 3 10))

(defn- journal-days
  "시작일부터 오늘까지 일수."
  []
  (.between ChronoUnit/DAYS journal-start (LocalDate/now)))

(defn- count-files [dir ext]
  (let [^java.io.File d (io/file dir)]
    (if (.isDirectory d)
      (->> (file-seq d)
           (filter (fn [^java.io.File f] (and (.isFile f) (.endsWith (.getName f) (str ext)))))
           count)
      0)))

(defn- home [& parts]
  (apply str (System/getProperty "user.home") parts))

(defn collect
  "Gather stats. Cached 1 hour."
  []
  (let [c @cache]
    (if (and c (< (- (System/currentTimeMillis) (:ts c)) ttl-ms))
      (:data c)
      (let [data {:notes       (count-files (home "/sync/org") ".org")
                  :bibliography 8208  ;; TODO: bibcli query
                  :commits      8557  ;; TODO: git count
                  :journal     (journal-days)
                  :health       4489  ;; TODO: lifetract.db query
                  :garden      (count-files (home "/repos/gh/notes/content") ".md")}]
        (reset! cache {:data data :ts (System/currentTimeMillis)})
        data))))
