(ns skos-explorer.clientside
  (:require [enfocus.core :as ef]
            [clojure.browser.repl :as repl])
  (:require-macros [enfocus.macros :as em]))

(repl/connect "http://localhost:9000/repl")

(js/console.log "Hallo der, mister Åsen.")
