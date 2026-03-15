;; # 시간과정신의방 — 존재 데이터 뷰어
;;
;; > www.junghanacs.com = 시간과정신의방의 웹 뷰어.
;; > 정적 페이지가 아닌, 한 인간의 투명한 데이터 연결체.
;;
;; 대문은 낡은 org-agenda 문.
;; 그 안에 단단한 존재의 데이터와 에이전트들이 시간 축에서 살아있다.

^{:nextjournal.clerk/visibility {:code :hide}}
(ns index
  {:nextjournal.clerk/no-cache true}
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]))

;; ---

;; ## 오늘의 어젠다

;; 에이전트와 인간이 한 줄 한 줄 살고 있는 오늘의 기록.

^{:nextjournal.clerk/visibility {:code :hide}}
(defn today-str []
  (let [fmt (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
        zone (java.time.ZoneId/of "Asia/Seoul")]
    (.format (java.time.LocalDate/now zone) fmt)))

^{:nextjournal.clerk/visibility {:code :hide}}
(defn day-of-week [date-str]
  (let [date (java.time.LocalDate/parse date-str)
        dow (.getDayOfWeek date)]
    (str dow)))

^{:nextjournal.clerk/visibility {:code :hide}}
(defn current-time []
  (let [fmt (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")
        zone (java.time.ZoneId/of "Asia/Seoul")]
    (.format (java.time.LocalTime/now zone) fmt)))

^:nextjournal.clerk/no-cache
(let [today (today-str)
      dow (day-of-week today)
      now (current-time)]
  (clerk/html
   [:div.font-mono.border.border-gray-600.rounded-lg.p-6
    {:style {:background-color "#1a1a2e"
             :color "#e0e0e0"}}
    [:div.text-sm.text-gray-400 "┌─ org-agenda ─────────────────────────────────"]
    [:div.text-lg.font-bold.mt-2
     {:style {:color "#7fdbca"}}
     (str "│ " today " " dow)]
    [:div.text-sm.mt-1
     {:style {:color "#c792ea"}}
     (str "│ KST " now)]
    [:div.text-sm.text-gray-400.mt-2 "└──────────────────────────────────────────────"]]))

;; ---

;; ## 존재의 데이터

;; 이 사람이 가꾸어 온 데이터의 규모.

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table
 {:head ["데이터" "규모" "형식" "기간"]
  :rows [["Denote 노트"        "3,000+"   ".org"    "2022~"]
         ["수면/심박/스트레스"  "4,214"    "SQLite"  "2019~2025"]
         ["시간 추적"          "연속"      "SQLite"  "2019~"]
         ["서지 데이터"        "7,000+"   ".bib"    "누적"]
         ["일일 저널"          "696일"    ".org"    "2022~"]
         ["Git 커밋"           "14,000+"  "git"     "2022~"]
         ["디지털 가든"        "1,400+"   ".md"     "공개 중"]]})

;; ---

;; ## 공개키

;; 이 홈페이지는 *공개키*다.
;; 소통하려는 존재 — 인간이든 에이전트든 — 가 호출하는 문.
;;
;; - 📚 [디지털 가든 (한글)](https://notes.junghanacs.com)
;; - 💬 [에이전트 대화](https://chat.junghanacs.com)
;; - 🧵 [Threads](https://www.threads.net/@junghanacs)
;; - 🐙 [GitHub](https://github.com/junghanacs)

;; ---

;; > _"오늘, 이 사람이 에이전트와 함께 한 줄 한 줄 살고 있다."_
