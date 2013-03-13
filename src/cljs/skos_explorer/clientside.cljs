(ns skos-explorer.clientside
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :refer [by-id log]]
            [domina.events :refer [listen!]]
            [goog.net.XhrIo :as xhr]
            [goog.style :as style]))

;(repl/connect "http://localhost:9000/repl")

        ;(if-let [l (get-in r [:highlight :labels])]
         ; (clojure.string/join l)

(defn search-results [results]
  (clojure.string/join
    (for [r (results :hits)]
      (str "<tr><td class='description'>"
           (when-let [d (get-in r[:highlight :description])]
             (apply str d))
           "</td><td class='labels'>"
            (when-let [l (get-in r[:highlight :labels])]
             (clojure.string/join ", " l))
           "</td>"
           "<td class='title'><a href='/?uri=" (r :_id) "'>"
           (get-in r [:_source :title])
           "</a></td></tr>"))))

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
    (log results)
    (set! (.-innerHTML (by-id "search-body")) (search-results results))
    (set! (.-innerHTML (by-id "num-hits")) (results :total))))

(defn searching [event]
  (let [s (.-value (by-id "search"))]
    (if (<= 2 (count s))
      (do
        (style/setStyle (by-id "search-results") "display" "block")
        (edn-call "/search" searched "POST" {:term s :offset 0 :limit 15}))
      (style/setStyle (by-id "search-results") "display" "none"))))

(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (listen! (by-id "search") :keyup searching))