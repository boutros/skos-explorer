(ns skos-explorer.views
  (:require [net.cgrand.enlive-html :as html]))

(defn links
  [linkmap uri label]
  (html/clone-for [n linkmap]
    (html/content
      {:tag :a, :attrs {:href (str "/skos/" (re-find #"[0-9]*$"(uri n)))},
       :content [(label n)]})))

(html/deftemplate concept
  "public/skos.html"
  [uri bindings narrower broader related topconcepts]

  [:h2] (if (some #(= (str uri) %) (map :concept topconcepts))
          (html/content (str "Top concept: " (->> bindings :preflabel first)))
          (html/content (str "Concept: " (->> bindings :preflabel first))))
  [:#uri] (html/content (str uri))
  [:#updated] (html/content (->> bindings :modified first))
  [:#comment] (html/content (->> bindings :comment first))
  [:#scope] (html/content (->> bindings :scope first))
  [:ul.narrower :li] (links narrower :narrower :narrowerlabel)
  [:ul.related :li] (links related :related :relatedlabel)
  [:#broader-or-top] (if (some #(= (str uri) %) (map :concept topconcepts))
                       (html/content "other top concepts")
                       (html/content "broader concepts"))
  [:ul.broader :li] (if (some #(= (str uri) %) (map :concept topconcepts))
                      (links (remove #(= (str uri) (:concept %)) topconcepts)
                             :concept :label)
                      (links broader :broader :broaderlabel))
  [:#alternate :li] (html/clone-for [n (bindings :altlabel)]
                                    (html/content n))
  [:#hidden :li] (html/clone-for [n (bindings :hiddenlabel)]
                                    (html/content n))
  [:#related :li] (html/clone-for [n (bindings :relatedlabel)]
                                    (html/content n))
  [:#prefered :li] (html/content (->> bindings :preflabel first))
  [:.links :p] (html/clone-for [n (bindings :link)]
                               (html/content
                                 {:tag :a, :attrs {:href n}, :content [n]})))