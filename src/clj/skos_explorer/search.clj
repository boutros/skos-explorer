(ns skos-explorer.search
  (:require [clj-elasticsearch.client :refer [make-client search]]))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(defonce es
  (make-client :transport {:hosts [(config :elastic-host)]
                           :cluster-name (config :elastic-cluster)}))

(defn search-query
  [term offset limit]
  {:query
   {:multi_match {:fields ["title" "labels" "description"]
                  :query term}}
    :highlight {:fields {"description" {} "labels" {} }}
    :from offset
    :size limit})

(defn results
  [term offset limit]
  (let [results (search es {:indices [(config :elastic-index)]
                            :types [(config :elastic-type)]
                            :extra-source (search-query term offset limit)})]
    {:took (results :took)
     :hits (get-in results [:hits :hits])
     :total (get-in results [:hits :total])
     :offset offset :limit limit}))