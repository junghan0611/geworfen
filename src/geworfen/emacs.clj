(ns geworfen.emacs
  "Emacs agent-server bridge — calls emacsclient, returns parsed results.
   This is the Emacs-way: we call the same functions that humans and bots use."
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn eval-elisp
  "Evaluate elisp via emacsclient connected to agent-server.
   Returns the raw string result (with quotes stripped)."
  [expr]
  (let [{:keys [out err exit]} (sh "emacsclient" "-s" "agent-server" "--eval" expr)]
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
  "Get org-agenda week view."
  ([]
   (eval-elisp "(agent-org-agenda-week)"))
  ([date-str]
   (eval-elisp (format "(agent-org-agenda-week \"%s\")" date-str))))

(defn alive?
  "Check if agent-server is reachable."
  []
  (try
    (= "2" (str/trim (:out (sh "emacsclient" "-s" "agent-server" "--eval" "(+ 1 1)"))))
    (catch Exception _ false)))
