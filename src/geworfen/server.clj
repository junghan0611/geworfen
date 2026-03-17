(ns geworfen.server
  "HTTP server — http-kit + reitit.
   100 visitors hitting /api/agenda?date=2026-03-15 = 1 emacsclient call (cached)."
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [ring.util.response :as resp]
            [ring.middleware.params :refer [wrap-params]]
            [geworfen.agenda :as agenda]
            [geworfen.stats :as stats]
            [jsonista.core :as json]))

(defonce ^:private server (atom nil))

(defn- agenda-handler
  "GET /api/agenda?date=2026-03-17  (or today if omitted)"
  [request]
  (let [date (or (get-in request [:query-params "date"])
                 (str (java.time.LocalDate/now)))
        data (agenda/get-day date)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"
               "Access-Control-Allow-Origin" "*"}
     :body (json/write-value-as-string data)}))

(defn- stats-handler
  "GET /api/stats — existence data counts"
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"
             "Access-Control-Allow-Origin" "*"}
   :body (json/write-value-as-string (stats/collect))})

(defn- trigger-handler
  "POST /api/trigger — agent stamps, invalidate today's cache"
  [_request]
  (agenda/invalidate-today!)
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"triggered\": true}"})

(defn- index-handler [_request]
  (-> (resp/resource-response "public/index.html")
      (resp/content-type "text/html; charset=utf-8")))

(def app
  (ring/ring-handler
   (ring/router
    [["/"          {:get {:handler index-handler}}]
     ["/api"
      ["/agenda"  {:get {:handler agenda-handler}}]
      ["/stats"   {:get {:handler stats-handler}}]
      ["/trigger" {:post {:handler trigger-handler}}]]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler))
   {:middleware [[ring.middleware.params/wrap-params]]}))

(defn start!
  [{:keys [port] :or {port 8080}}]
  (when @server (@server) (reset! server nil))
  (reset! server (http/run-server app {:port port}))
  (println (str "geworfen listening on http://localhost:" port)))

(defn stop! []
  (when @server (@server) (reset! server nil) (println "stopped")))
