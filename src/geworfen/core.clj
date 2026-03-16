(ns geworfen.core
  "geworfen — existence data WebTUI viewer entry point"
  (:require [geworfen.server :as server])
  (:gen-class))

(defn -main
  "Start the server"
  [& _args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (server/start! {:port port})
    (println (str "geworfen listening on http://localhost:" port))))
