(ns user
  (:require [nextjournal.clerk :as clerk]))

(comment
  ;; Start Clerk with file watcher, open browser
  (clerk/serve! {:browse? true
                 :port 7777
                 :watch-paths ["notebooks" "src"]})

  ;; Show a specific notebook
  (clerk/show! "notebooks/index.clj")

  ;; Build static HTML (for testing)
  (clerk/build! {:paths ["notebooks/index.clj"]
                 :out-path "public/clerk"})

  ;; Clear cache
  (clerk/clear-cache!))
