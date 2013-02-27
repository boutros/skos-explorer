(ns skos-explorer.routes
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [net.cgrand.enlive-html :as html]
            [skos-explorer.sparql :as sparql])
  (:import java.net.URI))

(html/deftemplate concept
  "public/skos.html" [uri bindings narrower broader related topconcepts]
  [:h2] (if (some #(= (str uri) %) (map :concept topconcepts))
          (html/content (str "Top concept: " (->> bindings :preflabel first)))
          (html/content (str "Concept: " (->> bindings :preflabel first))))
  [:#uri] (html/content (str uri))
  [:#updated] (html/content (->> bindings :modified first))
  [:#comment] (html/content (->> bindings :comment first))
  [:#scope] (html/content (->> bindings :scope first))
  [:ul.narrower :li] (html/clone-for
                       [n narrower]
                       (html/content
                         {:tag :a, :attrs {:href (str "/skos/" (re-find #"[0-9]*$"(:narrower n)))},
                          :content [(:narrowerlabel n)]}))
  [:ul.related :li] (html/clone-for
                       [n related]
                       (html/content
                         {:tag :a, :attrs {:href (str "/skos/" (re-find #"[0-9]*$"(:related n)))},
                          :content [(:relatedlabel n)]}))
  [:#broader-or-top] (if (some #(= (str uri) %) (map :concept topconcepts))
                       (html/content "other top concepts")
                       (html/content "broader concepts"))
  [:ul.broader :li] (if (some #(= (str uri) %) (map :concept topconcepts))
                      (html/clone-for [n (remove #(= (str uri) (:concept %)) topconcepts)]
                        (html/content
                          {:tag :a, :attrs {:href (str "/skos/" (re-find #"[0-9]*$"(:concept n)))},
                            :content [(:label n)]}))
                      (html/clone-for
                        [n broader]
                        (html/content
                          {:tag :a, :attrs {:href (str "/skos/" (re-find #"[0-9]*$" (:broader n)))},
                           :content [(:broaderlabel n)]})))
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

(defroutes main-routes
  (GET "/" [] "index")
  (GET ["/skos/:id", :id #"[0-9]+"]
       [id]
       (let [uri (URI. (str "http://vocabulary.curriculum.edu.au/scot/" id))
             topconcepts (->> (sparql/fetch-top-concepts) sparql/solutions (sparql/extract [:concept :label]))
             res (sparql/fetch uri)
             s (sparql/solutions res)
             b (sparql/bindings res)
             narrower (sparql/extract [:narrower :narrowerlabel] s)
             broader (sparql/extract [:broader :broaderlabel] s)
             related (sparql/extract [:related :relatedlabel] s)]
         (concept uri b narrower broader related topconcepts)))
  (route/resources "/")
  (route/not-found "Page not found"))

(def server
  (handler/api main-routes))
