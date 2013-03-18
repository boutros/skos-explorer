(ns skos-explorer.routes
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [skos-explorer.views :as views]
            [skos-explorer.sparql :as sparql]
            [skos-explorer.search :as search]
            [ring.middleware.edn :refer [wrap-edn-params]])
  (:import java.net.URI))

(defonce config
  (read-string (slurp "resources/config.edn")))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes main-routes
  (GET "/"
       [uri]
       (let [uri (URI. (or uri (config :default-uri)))
             topconcepts (->> (sparql/fetch-top-concepts) sparql/solutions (sparql/extract [:concept :label]))
             res (sparql/fetch uri)
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
             related (sparql/extract [:related :relatedlabel] s)]
         (views/concept uri b comments note scope example prefered alternate hidden narrower broader related topconcepts)))
  (POST "/search"
        [term offset limit]
        (generate-response (search/results term offset limit)))
  (route/resources "/")
  (route/not-found "Page not found"))

(def server
  (handler/api (wrap-edn-params main-routes)))