(ns skos-explorer.clientside
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :refer [by-id by-class log set-value! attr remove-attr!
                            set-attr! remove-class! add-class! text set-text!]]
            [domina.events :refer [listen! raw-event target dispatch!
                                   prevent-default]]
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
    (set! (.-innerHTML (by-id "search-body")) (search-results results))
    (set! (.-innerHTML (by-id "num-hits")) (str (results :total) " (" (/ (results :took) 1000) " sekunder)"))
    (set-value! (by-id "offset") (results :offset))
    (set-value! (by-id "limit") (results :limit))
    (if (> (results :total) (+ (results :offset) (results :limit)))
      (remove-attr! (by-id "search-next") "disabled")
      (set-attr! (by-id "search-next") "disabled"))
    (if (> (results :offset) 0)
      (remove-attr! (by-id "search-prev") "disabled")
      (set-attr! (by-id "search-prev") "disabled"))))

(defn searching [event]
  (let [s (.-value (by-id "search"))
        keycode (.-keyCode (raw-event event))]
    (if (= keycode 27)
      (do
        (style/setStyle (by-id "search-results") "display" "none")
        (set! (.-value (by-id "search")) ""))
    (if (<= 2 (count s))
      (do
        (style/setStyle (by-id "search-results") "display" "block")
        (edn-call "/search" searched "POST" {:term s :offset 0 :limit 15}))
      (style/setStyle (by-id "search-results") "display" "none")))))

(defn search-next [event]
  (let [s (.-value (by-id "search"))
        offset (js/parseInt (.-value (by-id "offset")))
        limit (js/parseInt (.-value (by-id "limit")))]
    (edn-call "/search" searched "POST" {:term s :offset (+ offset limit) :limit limit})))

(defn search-prev [event]
  (let [s (.-value (by-id "search"))
        offset (js/parseInt (.-value (by-id "offset")))
        limit (js/parseInt (.-value (by-id "limit")))]
    (edn-call "/search" searched "POST" {:term s :offset (max (- offset limit) 0) :limit limit})))

(defn label-focus [event]
  (let [n (target event)
        lang (attr n :data-original-lang)
        original (attr n :data-original-value)]
    (set-text! n (str \" original \" "@" lang))))

(defn label-save [n value lang]
  (do
    ;(edn-call sparql update goes here)
    (set-attr! n :data-original-value value)
    (set-attr! n :data-original-lang lang)
    (remove-class! n "editing")
    (.focus (by-id"search"))
    (.blur (by-id "search"))))

(defn label-edit [event]
  (let [n (target event)
        keycode (.-keyCode (raw-event event))
        lang (attr n :data-original-lang)
        original (attr n :data-original-value)
        edited-text (last (re-find #"\"(.+)\"" (text n))) ;"
        edited-lang (last (re-find #"@(.+)$" (text n)))]
    (if (= (text n) (str \" original \" "@" lang))
      (remove-class! n "editing")
      (add-class! n "editing"))
    (when (= keycode 13)
      (prevent-default event)
      (label-save n edited-text edited-lang))
    (when (= keycode 27)
      (remove-class! n "editing")
      (set-text! n original)
      (.focus (by-id"search"))
      (.blur (by-id "search")))))

(defn label-blur [event]
  (let [n (target event)
        original (attr n :data-original-value)]
    (remove-class! n "editing")
    (set-text! n original)))

(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (listen! (by-id "search") :keyup searching)
  (listen! (by-id "search-next") :click search-next)
  (listen! (by-id "search-prev") :click search-prev)
  (listen! (by-class "label") :focus label-focus)
  (listen! (by-class "label") :blur label-blur)
  (listen! (by-class "label") :keyup label-edit)
  (listen! (by-class "label") :keydown label-edit))