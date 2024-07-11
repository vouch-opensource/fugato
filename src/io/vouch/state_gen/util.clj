;; Copyright © 2024 Vouch.io LLC

(ns io.vouch.state-gen.util)

;; =============================================================================
;; Debug Helpers

(defn seq-rose
  "Convert a rose tree into a printable list for visual debugging."
  [rose]
  (list (rose/root rose) (map seq-rose (rose/children rose))))
