(ns skos-explorer.clientside
  (:require [clojure.browser.repl :as repl]
            [dommy.template :as html]
            [dommy.core :as dom])
  (:require-macros [dommy.core-compile :refer [sel sel1]]
                   [dommy.template-compile :refer [node deftemplate]]))

(repl/connect "http://localhost:9000/repl")

(defn log [& more]
  (.log js/console (apply str more)))

(defn click!
  "Simulates a click event on node"
  [node]
  (let [event (.createEvent js/document "MouseEvents")]
    (.initMouseEvent event "click" true true)
    (.dispatchEvent node event)))

(defn ^:export init []
  (log "Hallo der, mister Åsen."))
