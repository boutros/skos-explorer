(ns skos-explorer.routes
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [net.cgrand.enlive-html :as html]
            [skos-explorer.sparql :as sparql])
  (:import java.net.URI))

(defroutes main-routes
  (GET "/" [] "index")
  (GET ["/skos/:id", :id #"[0-9]+"]
       [id]
       (str (sparql/concept
              (URI. (str "http://vocabulary.curriculum.edu.au/scot/" id)))))
  (route/resources "/")
  (route/not-found "Page not found"))

(def server
  (handler/api main-routes))
