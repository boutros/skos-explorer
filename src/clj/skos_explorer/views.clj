(ns skos-explorer.views
  (:require [net.cgrand.enlive-html :as html]))

(defn links
  [linkmap uri label]
  (html/clone-for [n linkmap]
    (html/content
      [{:tag :a, :attrs {:href (str "/skos/" (re-find #"[0-9]*$"(uri n)))},
       :content [(label n)]}
      {:tag :div :attrs {:class "circle minus"}
       :content [{:tag :p :content "-"}]}])))

(html/deftemplate concept
  "public/skos.html"
  [uri bindings narrower broader related topconcepts]

  [:h2] (html/content (str
          (if (some #(= (str uri) %) (map :concept topconcepts))
            "Top concept: "
            "Concept: ")
            (->> bindings :preflabel first)))
  [:#uri] (html/content (str uri))
  [:#updated] (html/content (->> bindings :modified first))
  [:#comment] (html/content (->> bindings :comment first))
  [:#scope] (html/content (->> bindings :scope first))
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
  [:#alternate :li.concept] (html/clone-for [n (bindings :altlabel)]
                                    (html/content n))
  [:#hidden :li.concept] (html/clone-for [n (bindings :hiddenlabel)]
                                    (html/content n))
  [:#related :li.concept] (html/clone-for [n (bindings :relatedlabel)]
                                    (html/content n))
  [:#prefered :li.concept] (html/content (->> bindings :preflabel first))
  [:.links :p.first] (html/clone-for [n (bindings :link)]
                               (html/content
                                 {:tag :a, :attrs {:href n}, :content [n]})))