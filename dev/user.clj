(ns user
  (:require [geworfen.server :as server]))

(comment
  ;; 서버 시작
  (server/start! {:port 8080})

  ;; 서버 중지
  (server/stop!)

  ;; 포트 변경해서 시작
  (server/start! {:port 3000}))
