(ns geworfen.stats
  "Existence data stats — simple file counts."
  (:require [clojure.java.io :as io]
            [geworfen.db :as db]
            [geworfen.emacs :as emacs])
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
                  ;; lifetract.db distinct sleep days. nil if DB unreachable.
                  :health      (db/sleep-days)
                  ;; 로컬 디렉토리는 notes, GitHub 원격은 junghan0611/garden.
                  ;; 레포를 재클론하면 inode가 바뀌어 컨테이너의 bind mount가
                  ;; 삭제된 디렉토리를 계속 봐서 0이 된다 → 컨테이너 재생성.
                  :garden      (count-files (home "/repos/gh/notes/content") ".md")
                  ;; 버전 표면 — footer가 그린다. 스큐가 페이지에서 보이라고 둔다
                  ;; (2026-09-02 emacsclient/server 스큐 사건, ops/README.md).
                  ;; 둘 다 nil 가능: 데몬이 죽어도 /api/stats 는 살아야 한다 —
                  ;; 이 endpoint 는 emacs 무관한 것이 원래 성질이었고, 여기 붙이는
                  ;; 두 호출은 coreutils timeout 으로 묶여 그 성질을 깨지 않는다.
                  :versions    {:emacs-server (emacs/server-version)
                                :emacs-client (emacs/client-version)}}]
        (reset! cache {:data data :ts (System/currentTimeMillis)})
        data))))
