(ns skos-explorer.sparql
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [clj-http.client :as client]
            [boutros.matsu.sparql :refer :all]
            [boutros.matsu.core :refer [register-namespaces]]
            [cheshire.core :refer [parse-string]]
            [clojure.walk :refer [keywordize-keys]])
  (:import java.net.URI))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(register-namespaces {:skos "<http://www.w3.org/2004/02/skos/core#>"
                      :dc "<http://purl.org/dc/terms/>"
                      :owl "<http://www.w3.org/2002/07/owl#>"
                      :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"})

(defn languages [v]
  (interpose || (for [l (config :languages)]
                 (lang-matches (lang v) l))))

(defn concept
  "SPARQL-query to get relevant properites from a skos:Concept"
  [uri]
  (query
    (base (URI. "http://www.w3.org/2004/02/skos/core#"))
    (select-reduced :created :preflabel :altlabel :hiddenlabel :scopenote :comment
                    :narrower :narrowerlabel :broader :broaderlabel
                    :related :relatedlabel :modified :link :note :example)
    (from (URI. (config :graph)))
    (where uri a [:Concept] \;
           [:prefLabel] :preflabel \;
           [:dc :created] :created \.
           (filter (languages :preflabel))
           (optional uri [:dc :modified] :modified)
           (optional uri [:rdfs :comment] :comment)
           (optional uri [:exactMatch] :link)
           (optional uri [:narrower] :narrower \.
                     :narrower [:prefLabel] :narrowerlabel
                     (filter (languages :narrowerlabel)))
           (optional uri [:broader] :broader \.
                     :broader [:prefLabel] :broaderlabel
                     (filter (languages :broaderlabel)))
           (optional uri [:related] :related \.
                     :related [:prefLabel] :relatedlabel
                     (filter (languages :relatedlabel)))
           (optional uri [:altLabel] :altlabel
                     (filter (languages :altlabel)))
           (optional uri [:hiddenLabel] :hiddenlabel)
           (optional uri [:scopeNote] :scopenote)
           (optional uri [:note] :note)
           (optional uri [:example] :example))))

(defquery top-concepts
  (select-distinct :concept :label)
  (where :concept [:skos :topConceptOf] (URI. (config :dataset)) \;
                  [:skos :prefLabel] :label
          (filter (lang-matches (lang :label) "en"))))

(defn fetch
  "Perform SPARQL query"
  [uri]
  (client/get (config :endpoint)
              {:query-params {"query" (concept uri)
                              "format" "application/sparql-results+json"}}))

(defn fetch-top-concepts
  "Returns the URIs of skos:topConcept"
  []
  (client/get (config :endpoint)
              {:query-params {"query" (query top-concepts)
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
  [response]
  (for [solution
        (->> response :body parse-string keywordize-keys :results :bindings)]
    (into {}
          (for [[k v] solution]
            [k (:value v)]))))

(defn extract
  "Extract selected variables from solutions. Return set of maps.

    ex: (extract [:a :b :c] solutions)
    => #{{:a 1} {:a 2} {:b 3} ..}"
  [vars solutions]
  (set (remove empty? (map #(select-keys % vars) solutions))))

(defn solutions-with-lang
  "Solutions with language tags"
  [response]
  (for [solution
        (->> response :body parse-string keywordize-keys :results :bindings)]
    (into {}
          (for [[k v] solution]
            [k {:value (:value v) :lang (:xml:lang v)}]))))