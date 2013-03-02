(ns skos-explorer.clientside
  (:require [clojure.browser.repl :as repl]
            [domina :refer [by-id value log]]))

(repl/connect "http://localhost:9000/repl")

(log "Hallo der, mister Åsen.")


