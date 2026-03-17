(ns geworfen.stats
  "Existence data stats — simple file counts."
  (:require [clojure.java.io :as io]))

(defonce ^:private cache (atom nil))
(def ^:private ttl-ms (* 60 60 1000)) ;; 1 hour

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
                  :journal     (count-files (home "/sync/org/journal") ".org")
                  :health       4489  ;; TODO: lifetract.db query
                  :garden      (count-files (home "/repos/gh/notes/content") ".md")}]
        (reset! cache {:data data :ts (System/currentTimeMillis)})
        data))))
