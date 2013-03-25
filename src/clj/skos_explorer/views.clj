(ns skos-explorer.views
  (:require [net.cgrand.enlive-html :as html]))

(defn hours-format [date]
  (.format (java.text.SimpleDateFormat. "HH:mm:ss") date))

(defn date-format [date]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-DD HH:mm:ss") date))

(defn links
  [linkmap uri label]
  (html/clone-for [n linkmap]
    (html/content
      [{:tag :a, :attrs {:href (str "/?uri=" (uri n))},
       :content (label n)}
      {:tag :div :attrs {:class "circle minus"}
       :content [{:tag :p :content "-"}]}])))

(defn labels
  [coll k]
  (html/clone-for [n coll]
                  (html/do->
                    (html/content (-> n k :value))
                    (html/set-attr :data-original-value (-> n k :value))
                    (html/set-attr :data-original-lang (-> n k :lang)))))

(html/deftemplate concept
  "public/concept.html"
  [uri bindings comments note scope example prefered alternate hidden narrower broader related topconcepts timestamp]

  [:#heading] (html/content (str
                (if (some #(= (str uri) %) (map :concept topconcepts))
                  "Top concept: "
                  "Concept: ")
                (->> bindings :preflabel first)))
  [:#uri] (html/content {:tag :a :attrs {:href uri },
                         :content (str uri)})
  [:#created] (html/content (->> bindings :created first))
  [:#updated] (html/content (->> bindings :modified first))
  [:#comment :li.label] (labels comments :comment)
  [:#note :li.label] (labels note :note)
  [:#scope :li.label] (labels scope :scopenote)
  [:#example :li.label] (labels example :example)
  [:ul.narrower :li.concept] (links narrower :narrower :narrowerlabel)
  [:ul.related :li.concept] (links related :related :relatedlabel)
  [:#broader-or-top] (html/content
                       (if (some #(= (str uri) %) (map :concept topconcepts))
                         "Other top concepts"
                         "Broader concepts"))
  [:ul.broader :li.concept] (if (some #(= (str uri) %) (map :concept topconcepts))
                      (links (remove #(= (str uri) (:concept %)) topconcepts)
                             :concept :label)
                      (links broader :broader :broaderlabel))
  [:#alternate :li.label] (labels alternate :altlabel)
  [:#hidden :li.label] (labels hidden :hiddenlabel)
  [:#related :li.label] (html/clone-for [n (bindings :relatedlabel)]
                                    (html/content n))
  [:#prefered :li.label] (labels prefered :preflabel)
  [:.links :p.link] (html/clone-for [n (bindings :link)]
                               (html/content
                                 {:tag :a, :attrs {:href n}, :content n}))
  [:#logg-msg] (html/content (str "[" timestamp "] Concept loaded.")))

(html/deftemplate log
  "public/log.html"
  [transactions]

  [:#transactions :tr.logline]
  (html/clone-for [n transactions]
                  [:td.timestamp] (html/content (str "[" (-> n :time date-format)))
                  [:a.uri] (html/do->
                             (html/set-attr :href (str "/?uri="(-> n :message :concept)))
                             (html/content (str "<"(-> n :message :concept) ">")))
                  [:span.desc] (html/content (-> n :message :description))
                  [:td.description :span.query] (html/content (-> n :message :query))
                  [:td.undo :span.query] (html/content (-> n :message :undo))))
