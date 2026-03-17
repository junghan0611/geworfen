(ns build
  (:require [clojure.tools.build.api :as b]))

(def basis (delay (b/create-basis {:project "deps.edn"})))
(def class-dir "target/classes")
(def uber-file "target/geworfen.jar")

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"] :target-dir class-dir})
  (b/compile-clj {:basis     @basis
                   :src-dirs  ["src"]
                   :class-dir class-dir
                   :ns-compile '[geworfen.core
                                 geworfen.server
                                 geworfen.agenda
                                 geworfen.emacs
                                 geworfen.stats]})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     @basis
           :main      'geworfen.core}))
