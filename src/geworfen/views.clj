(ns geworfen.views
  "HTML 뷰 — WebTUI CSS 기반 SF 터미널 미학"
  (:require [hiccup2.core :as h]
            [hiccup.page :as page]))

;; ---------------------------------------------------------------------------
;; 레이아웃
;; ---------------------------------------------------------------------------

(defn layout
  "공통 HTML 레이아웃 — WebTUI CSS + SF 터미널 테마"
  [& body]
  (str
   (h/html
    (page/doctype :html5)
    [:html {:lang "ko" :data-theme "dark"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "게보르펜 — geworfen"]
      ;; WebTUI CSS
      [:link {:rel "stylesheet"
              :href "https://cdn.jsdelivr.net/npm/@aspect-ui/webtui@latest/dist/webtui.min.css"}]
      ;; 커스텀 SF 테마
      [:link {:rel "stylesheet" :href "/css/geworfen.css"}]
      ;; 폰트
      [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
      [:link {:rel "stylesheet"
              :href "https://fonts.googleapis.com/css2?family=Geist+Mono:wght@400;700&display=swap"}]]
     [:body
      body]])))

;; ---------------------------------------------------------------------------
;; 핸들러
;; ---------------------------------------------------------------------------

(defn index
  "대문 — org-agenda 타임라인"
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body
   (layout
    [:main
     [:div.tui-box.double
      [:header
       [:h1 "게보르펜"]
       [:p "던져진 것들 — 한 인간의 존재 데이터"]]]
     [:div.tui-box.single
      [:pre "TODO: org-agenda 타임라인"]]])})

(defn agenda-api
  "API — 오늘의 org-agenda 데이터 (JSON)"
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body "{\"status\": \"not-implemented\"}"})

(defn sse-events
  "SSE — 실시간 갱신 이벤트 스트림"
  [_request]
  ;; TODO: http-kit Channel + SSE 구현
  {:status 200
   :headers {"Content-Type" "text/event-stream"
             "Cache-Control" "no-cache"
             "Connection" "keep-alive"}
   :body "data: {\"type\": \"connected\"}\n\n"})

(defn trigger-refresh
  "트리거 — 에이전트가 스탬프 후 호출, SSE broadcast"
  [_request]
  ;; TODO: SSE broadcast 구현
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"triggered\": true}"})
