(ns skos-explorer.search
  (:require [clj-elasticsearch.client :refer [make-client search]]))

(defonce config
  (read-string (slurp "resources/config.edn")))

(defonce es
  (make-client :transport {:hosts [(config :elastic-host)]
                           :cluster-name (config :elastic-cluster)}))

(defn search-query
  [term]
  {:query
   {:multi_match {:fields ["title" "labels" "description"]
                  :query term}}
    :highlight {:fields {"description" {} "labels" {} }}
    :from 0
    :size 15})

(defn results
  [term]
  (select-keys ((search es {:indices [(config :elastic-index)]
                            :types [(config :elastic-type)]
                            :extra-source (search-query term)}) :hits)
               [:hits :total]))