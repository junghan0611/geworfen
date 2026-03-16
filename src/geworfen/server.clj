(ns geworfen.server
  "HTTP 서버 — http-kit + reitit"
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [geworfen.views :as views]))

(defonce ^:private server (atom nil))

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get {:handler views/index}}]
     ["/api"
      ["/agenda" {:get {:handler views/agenda-api}}]
      ["/events" {:get {:handler views/sse-events}}]
      ["/trigger" {:post {:handler views/trigger-refresh}}]]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler))
   {:middleware [[wrap-defaults
                  (assoc-in site-defaults [:security :anti-forgery] false)]]}))

(defn start!
  "서버 시작"
  [{:keys [port] :or {port 8080}}]
  (when @server
    (@server)
    (reset! server nil))
  (reset! server (http/run-server app {:port port}))
  (println (str "서버 리스닝: " port)))

(defn stop!
  "서버 중지"
  []
  (when @server
    (@server)
    (reset! server nil)
    (println "서버 중지됨")))
