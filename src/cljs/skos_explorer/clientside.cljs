(ns skos-explorer.clientside
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :as dom :refer [by-id by-class log]]
            [domina.events :as event]
            [goog.net.XhrIo :as xhr]
            [goog.style :as style]))

;(repl/connect "http://localhost:9000/repl")

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
    (dom/set-value! (by-id "offset") (results :offset))
    (dom/set-value! (by-id "limit") (results :limit))
    (if (> (results :total) (+ (results :offset) (results :limit)))
      (dom/remove-attr! (by-id "search-next") "disabled")
      (dom/set-attr! (by-id "search-next") "disabled"))
    (if (> (results :offset) 0)
      (dom/remove-attr! (by-id "search-prev") "disabled")
      (dom/set-attr! (by-id "search-prev") "disabled"))))

(defn updated [event]
  (let [response (.-target event)
        results (reader/read-string (.getResponseText response))]
    (dom/set-text! (by-id "logg-msg") (str "[" (results :timestamp) "] " (results :description)))
    (style/setStyle (by-id "undo-link") "display" "inline")))

(defn searching [event]
  (let [s (.-value (by-id "search"))
        keycode (.-keyCode (event/raw-event event))]
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
  (let [n (event/target event)
        lang (dom/attr n :data-original-lang)
        original (dom/attr n :data-original-value)
        rnge (.createRange js/document)
        sel (.getSelection js/window)]
    (dom/set-text! n original)
    (. rnge selectNodeContents n)
    (. sel removeAllRanges)
    (. sel addRange rnge)))

(defn label-save [n value lang]
  (let [uri (dom/text (by-id "uri"))
        property (-> n .-parentElement .-parentElement .-parentElement (dom/attr "id"))
        old-value (dom/attr n :data-original-value)
        old-lang (dom/attr n :data-original-value)]
    (if (= 0 (.-length value))
      (if (not= "" old-value)
        (do
          (edn-call "/delete" updated "PUT" {:concept uri :property property :value old-value :lang old-lang})
          (dom/destroy! n))
        (dom/destroy! n))
      (do
        (if (= 0 (.-length old-value))
          (edn-call "/add" updated "PUT" {:concept uri :property property :value value :lang lang})
          (edn-call "/update" updated "PUT" {:concept uri :property property :oldv old-value :oldl old-lang :newv value :newl lang}))
        (dom/set-attr! n :data-original-value value)
        (dom/set-attr! n :data-original-lang lang)
        (dom/remove-class! n "editing")
        (.focus (by-id"search"))
        (.blur (by-id "search"))))))

(defn label-edit [event]
  (let [n (event/target event)
        keycode (.-keyCode (event/raw-event event))
        lang (dom/attr n :data-original-lang)
        original (dom/attr n :data-original-value)
        edited-text (dom/text n)]
    (if (= (dom/text n) original)
      (dom/remove-class! n "editing")
      (dom/add-class! n "editing"))
    (when (= keycode 13)
      (event/prevent-default event)
      (label-save n edited-text lang))
    (when (= keycode 27)
      (dom/remove-class! n "editing")
      (dom/set-text! n original)
      (.focus (by-id"search"))
      (.blur (by-id "search")))))

(defn label-blur [event]
  (let [n (event/target event)
        original (dom/attr n :data-original-value)]
    (dom/remove-class! n "editing")
    (dom/set-text! n original)
    (when (= 0 (.-length original))
      (dom/destroy! n))))

(defn label-add [event]
  (do
    (dom/insert-before! (.-parentElement (.-parentElement (event/target event)))
                     (str "<li id=\"tmp\" class=\"label editing\" contenteditable=\"true\" data-original-value=\"edit me!\" "
                          "data-original-lang=\"no\">edit me!</li>"))
    (event/listen! (by-id "tmp") :blur label-blur)
    (event/listen! (by-id "tmp") :focus label-focus)
    (event/listen! (by-id "tmp") :keyup label-edit)
    (event/listen! (by-id "tmp") :keydown label-edit)
    (.focus (by-id "tmp"))
    (dom/set-attr! (by-id "tmp") "data-original-value" "")
    (dom/remove-attr! (by-id "tmp") "id")))


(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (event/listen! (by-id "search") :keyup searching)
  (event/listen! (by-id "search-next") :click search-next)
  (event/listen! (by-id "search-prev") :click search-prev)
  (event/listen! (by-class "label") :focus label-focus)
  (event/listen! (by-class "label") :blur label-blur)
  (event/listen! (by-class "label") :keyup label-edit)
  (event/listen! (by-class "label") :keydown label-edit)
  (event/listen! (by-class "add-label") :click label-add))