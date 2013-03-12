(ns skos-explorer.routes
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [skos-explorer.views :as views]
            [skos-explorer.sparql :as sparql]
            [ring.middleware.edn :refer [wrap-edn-params]])
  (:import java.net.URI))

(defonce config
  (read-string (slurp "resources/config.edn")))

(defroutes main-routes
  (GET ["/"]
       [uri]
       (let [uri (URI. (or uri (config :default-uri)))
             topconcepts (->> (sparql/fetch-top-concepts) sparql/solutions (sparql/extract [:concept :label]))
             res (sparql/fetch uri)
             s (sparql/solutions res)
             b (sparql/bindings res)
             narrower (sparql/extract [:narrower :narrowerlabel] s)
             broader (sparql/extract [:broader :broaderlabel] s)
             related (sparql/extract [:related :relatedlabel] s)]
         (views/concept uri b narrower broader related topconcepts)))
  (route/resources "/")
  (route/not-found "Page not found"))

(def server
  (handler/api (wrap-edn-params main-routes)))