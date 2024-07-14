;; Copyright © 2024 Vouch.io LLC

(ns fugato.util
  (:require [clojure.test.check.rose-tree :as rose]))

;; =============================================================================
;; Debug Helpers

(defn seq-rose
  "Convert a rose tree into a printable list for visual debugging."
  [rose]
  (list (rose/root rose) (map seq-rose (rose/children rose))))
