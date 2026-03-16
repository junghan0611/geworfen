(ns geworfen.server
  "HTTP server — http-kit + reitit"
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
  "Start the server on the given port"
  [{:keys [port] :or {port 8080}}]
  (when @server
    (@server)
    (reset! server nil))
  (reset! server (http/run-server app {:port port}))
  (println (str "Server listening on port " port)))

(defn stop!
  "Stop the server"
  []
  (when @server
    (@server)
    (reset! server nil)
    (println "Server stopped")))
