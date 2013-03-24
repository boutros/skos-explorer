(defproject skos-explorer "0.1.0-SNAPSHOT"
  :description "Exploring a RDF SKOS-landscape"
  :url "http://github.com/boutros/skos-explorer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [compojure "1.1.5"]
                 [fogus/ring-edn "0.2.0-SNAPSHOT"]
                 [enlive "1.1.1"]
                 [clj-http "0.6.4"]
                 [clj-log "0.4.5"]
                 [cheshire "5.0.1"]
                 [matsu "0.1.1-SNAPSHOT"]
                 [domina "1.0.2-SNAPSHOT"]
                 [clj-elasticsearch "0.4.0-RC1"]
                 [org.elasticsearch/elasticsearch "0.20.5"]]
  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "0.3.0"]]
  :ring {:handler skos-explorer.routes/server}
  :cljsbuild {:builds
              [{:source-paths ["src/cljs"],
                :compiler
                {:pretty-print true,
                 :output-to "resources/public/js/app.js",
                 :optimizations :whitespace}}]})