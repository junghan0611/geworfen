(ns user
  (:require [geworfen.server :as server]))

(comment
  ;; Start server
  (server/start! {:port 8080})

  ;; Stop server
  (server/stop!)

  ;; Start on a different port
  (server/start! {:port 3000}))
