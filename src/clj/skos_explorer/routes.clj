(ns skos-explorer.routes
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [net.cgrand.enlive-html :as html]))

(defroutes main-routes
  (GET "/" [] "index")
  (route/resources "/")
  (route/not-found "Page not found"))

(def server
  (handler/api main-routes))
