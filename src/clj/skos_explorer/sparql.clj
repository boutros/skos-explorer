(ns skos-explorer.sparql
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [clj-http.client :as client]
            [boutros.matsu.sparql :refer :all]
            [boutros.matsu.core :refer [register-namespaces]]
            [cheshire.core :refer [parse-string]]
            [clojure.walk :refer [keywordize-keys]])
  (:import java.net.URI))

(def endpoint "http://localhost:8890/sparql")

(register-namespaces {:skos "<http://www.w3.org/2004/02/skos/core#>"
                      :dc "<http://purl.org/dc/terms/>"
                      :owl "<http://www.w3.org/2002/07/owl#>"
                      :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"})

(defn get-concept
  "SPARQL-query to get relevant properites from a skos:Concept"
  [uri]
  (query
    (base (URI. "http://www.w3.org/2004/02/skos/core#"))
    (select-distinct :preflabel :altlabel :hiddenlabel :scopenote :narrower
                     :broader :related :modified)
    (where uri a [:Concept] \;
           [:prefLabel] :preflabel \;
           [:dc :modified] :modified
           (optional uri [:narrower] :narrower)
           (optional uri [:broader] :broader)
           (optional uri [:related] :related)
           (optional uri [:altLabel] :altlabel)
           (optional uri [:hiddenLabel] :hiddenlabel)
           (optional uri [:scopeNote] :scopenote)
           (filter (lang-matches (lang :preflabel) "en")))))

(defn fetch
  "Perform SPARQL query"
  [uri]
  (client/get endpoint
              {:query-params {"query" (get-concept uri)
                              "format" "application/sparql-results+json"}}))

(defn bindings
  "Returns the bindings of a sparql/json response in a map with the binding
  parameters as keys and results as sets:

    {:binding1 #{value1, value2} :binding2 #{v1, v2}}"
  [response] ; response = response from http-client
  (let [{{vars :vars} :head {solutions :bindings} :results}
        (->> response :body parse-string keywordize-keys)
        vars (map keyword vars)]
    (into {}
          (for [v vars]
            [v (set (keep #(->> % v :value) solutions))]))))

(defn solutions
  "Returns the solution maps from a sparql/json response."
  ;TODO
  )

(defn concept
  "Returns concept bindings"
  [uri]
  (bindings (fetch uri)))