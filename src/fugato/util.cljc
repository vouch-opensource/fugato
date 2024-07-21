;; Copyright © 2024 Vouch.io LLC

(ns fugato.util
  (:refer-clojure :exclude [flatten])
  (:require [clojure.test.check.rose-tree :as rose]))

;; =============================================================================
;; Debug Helpers

(defn seq-rose
  "Convert a rose tree into a printable list for visual debugging."
  [rose]
  (list (rose/root rose) (map seq-rose (rose/children rose))))

(defn flatten
  "Covert a rose tree in a flattened list for visual debugging"
  [rose]
  (map rose/root
    (tree-seq (fn [x] (seq (rose/children x))) rose/children rose)))
