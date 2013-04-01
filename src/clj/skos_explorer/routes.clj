(ns skos-explorer.routes
  (:require [compojure.core :refer [GET POST PUT defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [skos-explorer.views :as views]
            [skos-explorer.sparql :as sparql]
            [skos-explorer.search :as search]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [boutros.matsu.util :refer [pprint]])
  (:import java.net.URI))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn now [] (views/hours-format (java.util.Date.)))

(defroutes main-routes
  (GET "/"
       [uri]
       (let [uri (URI. (or uri (config :default-uri)))
             topconcepts (->> (sparql/fetch-top-concepts) sparql/solutions (sparql/extract [:concept :label]))
             res (sparql/fetch-concept uri)
             s (sparql/solutions res)
             s2 (sparql/solutions-with-lang res)
             b (sparql/bindings res)
             comments (sparql/extract [:comment] s2)
             note (sparql/extract [:note] s2)
             scope (sparql/extract [:scopenote] s2)
             example (sparql/extract [:example] s2)
             prefered (sparql/extract [:preflabel] s2)
             alternate (sparql/extract [:altlabel] s2)
             hidden (sparql/extract [:hiddenlabel] s2)
             narrower (sparql/extract [:narrower :narrowerlabel] s)
             broader (sparql/extract [:broader :broaderlabel] s)
             related (sparql/extract [:related :relatedlabel] s)
             timestamp (now)]
         (views/concept uri b comments note scope example prefered alternate hidden narrower broader related topconcepts timestamp)))
  (GET "/transactions" [uri]
       (let [transactions (-> (sparql/fetch-transactions) sparql/solutions)]
         (views/log transactions uri)))
  (POST "/search"
        [term offset limit]
        (generate-response (search/results term offset limit)))
  (PUT "/add"
       [concept property value lang]
       (let [query (sparql/add-query concept (read-string property) value (keyword lang))
             desc (str "Added " (last (re-find #"\[:(.*)\]" property )) " \"" value "\"")
             undo (sparql/delete-query concept (read-string property) value lang)
             timestamp (now)]
         (sparql/publish-log concept desc query undo)
         ;(sparql/publish-log concept {:event "sparql-update" :query (pprint query) :undo undo :description desc})
         (generate-response {:query query :undo undo :description desc :timestamp timestamp})))
  (PUT "/update"
       [concept property oldv oldl newv newl]
       (let [query (sparql/update-query concept (read-string property) oldv oldl newv newl)
             desc (str "Changed " (last (re-find #"\[:(.*)\]" property )) " \"" oldv "\" -> \"" newv "\"")
             undo (sparql/update-query concept (read-string property) newv newl oldv oldl)
             timestamp (now)]
         ;(log :info {:event "sparql-update" :query (pprint query) :undo (pprint undo) :description desc :concept concept})
         (generate-response {:query query :undo undo :description desc :timestamp timestamp})))
  (PUT "/delete"
       [concept property value lang]
       (let [query (sparql/delete-query concept (read-string property) value lang)
             desc (str "Removed " (last (re-find #"\[:(.*)\]" property )) " \"" value "\"")
             undo (sparql/add-query concept (read-string property) value lang)
             timestamp (now)]
         ;(log :info {:event "sparql-update" :query (pprint query) :undo (pprint undo) :description desc :concept concept})
         (generate-response {:query query :undo undo :description desc :timestamp timestamp})))
  (route/resources "/")
  (route/not-found "Page not found"))

(def server
  (handler/api (wrap-edn-params main-routes)))