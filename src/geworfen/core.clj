(ns geworfen.core
  "게보르펜 — 존재 데이터 WebTUI 뷰어 진입점"
  (:require [geworfen.server :as server])
  (:gen-class))

(defn -main
  "서버 시작"
  [& _args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (server/start! {:port port})
    (println (str "게보르펜 서버 시작: http://localhost:" port))))
