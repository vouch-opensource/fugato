(ns fugato.test-impl
  (:require [clojure.test :as test :refer [deftest is]]
            [fugato.impl :as impl]))

(deftest test-all-drop1
  (is (= '((2 3 4) (1 3 4) (1 2 4) (1 2 3))
         (impl/all-drop1 [1 2 3 4]))))

(comment

  (test/run-tests)

  )
