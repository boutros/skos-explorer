(ns skos-explorer.clientside
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [goog.net.XhrIo :as xhr]
            [goog.style :as style]
            [dommy.template :as html]
            [dommy.core :as dom])
  (:require-macros [dommy.core-compile :refer [sel sel1]]
                   [dommy.template-compile :refer [node deftemplate]]))

;(repl/connect "http://localhost:9000/repl")

(defn log [& more]
  (.log js/console (apply str more)))

(defn click!
  "Simulates a click event on node"
  [node]
  (let [event (.createEvent js/document "MouseEvents")]
    (.initMouseEvent event "click" true true)
    (.dispatchEvent node event)))

(defn live-listen!
  "Listen on all elements in container, currently elem can only be a tag"
  [container elem event-type f]
  (.addEventListener
    (.querySelector js/document container)
    (name event-type)
    (fn [evt]
      (when (= elem (->> evt .-target .-tagName .toLowerCase))
        (f evt))
    false))) ; capture

(defn fu [evt] (.log js/console (->> evt .-target .-parentElement .-parentElement)))

(defn edn-call
  [path callback method data]
  (xhr/send path
            callback
            method
            (pr-str data)
            (clj->js {"Content-Type" "application/edn"})))

(defn searched [event]
  (let [response (.-target event)
        results (reader/read-string (.getResponseText response))]
    (set! (.-innerHTML (sel1 "#search-results")) results)))

(defn searching [event]
  (let [s (.-value (sel1 "#search"))]
    (if (<= 2 (count s))
      (do
        (style/setStyle (sel1 "#search-results") "display" "block")
        (edn-call "/search" searched "POST" {:term s}))
      (style/setStyle (sel1 "#search-results") "display" "none"))))

(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (dom/listen! (sel1 "#search") :keyup searching))