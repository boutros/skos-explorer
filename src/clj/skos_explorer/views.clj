(ns skos-explorer.views
  (:require [net.cgrand.enlive-html :as html]))

(defn links
  [linkmap uri label]
  (html/clone-for [n linkmap]
    (html/content
      [{:tag :a, :attrs {:href (str "/?uri=" (uri n))},
       :content (label n)}
      {:tag :div :attrs {:class "circle minus"}
       :content [{:tag :p :content "-"}]}])))

(html/deftemplate concept
  "public/concept.html"
  [uri bindings narrower broader related topconcepts]

  [:#heading] (html/content (str
                (if (some #(= (str uri) %) (map :concept topconcepts))
                  "Top concept: "
                  "Concept: ")
                (->> bindings :preflabel first)))
  [:#uri] (html/content {:tag :a :attrs {:href uri },
                         :content (str uri)})
  [:#created] (html/content (->> bindings :created first))
  [:#updated] (html/content (->> bindings :modified first))
  [:#comment :li.label] (html/clone-for [n (->> bindings :comment)]
                              (html/content n))
  [:#note :li.label] (html/clone-for [n (->> bindings :note)]
                                     (html/content n))
  [:#scope :li.label] (html/clone-for [n (->> bindings :scopenote)]
                                      (html/content n))
  [:#example :li.label] (html/clone-for [n (->> bindings :example)]
                                        (html/content n))
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
  [:#alternate :li.label] (html/clone-for [n (bindings :altlabel)]
                                    (html/content n))
  [:#hidden :li.label] (html/clone-for [n (bindings :hiddenlabel)]
                                    (html/content n))
  [:#related :li.label] (html/clone-for [n (bindings :relatedlabel)]
                                    (html/content n))
  [:#prefered :li.label] (html/content (->> bindings :preflabel first))
  [:.links :p.link] (html/clone-for [n (bindings :link)]
                               (html/content
                                 {:tag :a, :attrs {:href n}, :content n})))