(ns geworfen.views
  "HTML views — WebTUI CSS with SF terminal aesthetics"
  (:require [hiccup2.core :as h]
            [hiccup.page :as page]))

;; ---------------------------------------------------------------------------
;; Layout
;; ---------------------------------------------------------------------------

(defn layout
  "Common HTML layout — WebTUI CSS + SF terminal theme"
  [& body]
  (str
   (h/html
    (page/doctype :html5)
    [:html {:lang "en" :data-theme "dark"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "geworfen — thrown into the world"]
      ;; WebTUI CSS
      [:link {:rel "stylesheet"
              :href "https://cdn.jsdelivr.net/npm/@aspect-ui/webtui@latest/dist/webtui.min.css"}]
      ;; Custom SF theme
      [:link {:rel "stylesheet" :href "/css/geworfen.css"}]
      ;; Fonts
      [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
      [:link {:rel "stylesheet"
              :href "https://fonts.googleapis.com/css2?family=Geist+Mono:wght@400;700&display=swap"}]]
     [:body
      body]])))

;; ---------------------------------------------------------------------------
;; Handlers
;; ---------------------------------------------------------------------------

(defn index
  "Front door — org-agenda timeline"
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body
   (layout
    [:main
     [:div.tui-box.double
      [:header
       [:h1 "geworfen"]
       [:p "thrown into the world — raw existence data"]]]
     [:div.tui-box.single
      [:pre "TODO: org-agenda timeline"]]])})

(defn agenda-api
  "API — today's org-agenda data (JSON)"
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body "{\"status\": \"not-implemented\"}"})

(defn sse-events
  "SSE — real-time update event stream"
  [_request]
  ;; TODO: http-kit Channel + SSE implementation
  {:status 200
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"
             "Connection" "keep-alive"}
   :body "data: {\"type\": \"connected\"}\n\n"})

(defn trigger-refresh
  "Trigger — called by agents after stamping, broadcasts via SSE"
  [_request]
  ;; TODO: SSE broadcast implementation
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"triggered\": true}"})
