(defproject skos-explorer "0.1.0-SNAPSHOT"
  :description "Exploring a RDF SKOS-landscape"
  :url "http://github.com/boutros/skos-explorer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]
                 [clj-http "0.6.4"]
                 [cheshire "5.0.1"]
                 [matsu "0.1.0-SNAPSHOT"]]
  :plugins [[lein-ring "0.8.3"]
            [thheller/lein-test-loop "0.3.0"]
            [lein-cljsbuild "0.3.0"]]
  :ring {:handler skos-explorer.routes/server}
  :cljsbuild {:builds
              [{:source-path "src/cljs"
                :compiler {:output-to "resources/public/js/app.js"
                           :optimizations :whitespace
                           :pretty-print true}}]})