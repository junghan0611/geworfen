(ns geworfen.server
  "HTTP server — http-kit + reitit.
   100 visitors hitting /api/agenda?date=2026-03-15 = 1 emacsclient call (cached)."
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.java.io :as io]
            [clojure.string :as str]
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

(def ^:private content-types
  {"html" "text/html; charset=utf-8"
   "css"  "text/css; charset=utf-8"
   "js"   "application/javascript; charset=utf-8"
   "json" "application/json; charset=utf-8"
   "woff2" "font/woff2"
   "pub"  "text/plain; charset=utf-8"
   "png"  "image/png"
   "svg"  "image/svg+xml"})

(defn- serve-resource
  "Serve a classpath resource. Works in JVM and GraalVM native-image."
  [path]
  (when-let [url (io/resource path)]
    (let [ext (last (clojure.string/split path #"\."))
          ctype (get content-types ext "application/octet-stream")]
      {:status 200
       :headers {"Content-Type" ctype
                 "Cache-Control" "no-cache"}
       :body (io/input-stream url)})))

(defn- index-handler [_request]
  (or (serve-resource "public/index.html")
      {:status 404 :body "not found"}))

(defn- static-handler
  "Serve static files from public/ on classpath."
  [request]
  (let [path (subs (:uri request) 1)]  ;; strip leading /
    (or (serve-resource (str "public/" path))
        {:status 404 :body "not found"})))

(def app
  (ring/ring-handler
   (ring/router
    [["/"          {:get {:handler index-handler}}]
     ["/api"
      ["/agenda"  {:get {:handler agenda-handler}}]
      ["/stats"   {:get {:handler stats-handler}}]
      ["/trigger" {:post {:handler trigger-handler}}]]])
   (ring/routes
    static-handler)
   {:middleware [[ring.middleware.params/wrap-params]]}))

(defn start!
  [{:keys [port] :or {port 8080}}]
  (when @server (@server) (reset! server nil))
  (reset! server (http/run-server app {:port port}))
  (println (str "geworfen listening on http://localhost:" port)))

(defn stop! []
  (when @server (@server) (reset! server nil) (println "stopped")))
