;; Copyright © 2024 Vouch.io LLC

(ns fugato.test-impl
  (:require [clojure.spec.gen.alpha :as gen]
            [clojure.test :as test :refer [deftest is]]
            [clojure.test.check.rose-tree :as rose]
            [fugato.core :as fugato]
            [fugato.impl :as impl]
            [fugato.test-state-gen :as state]))

(def init-state
  {:user-a  #{}
   :user-b  #{}
   :door    :locked
   :room-1  #{:key :user-a :user-b}
   :room-2  #{}})

(def commands
  '({:command :take-key, :args [:user-a]}
    {:command :drop-key, :args [:user-a]}
    {:command :take-key, :args [:user-a]}
    {:command :unlock-door, :args [:user-a]}))

(deftest test-all-drop1
  (is (= '((2 3 4) (1 3 4) (1 2 4) (1 2 3))
         (impl/all-drop1 [1 2 3 4]))))

(deftest test-commands-rose
  (let [rose (impl/commands-rose state/model init-state commands 1)]
    (is (= commands (rose/root rose)))
    (is (= 4 (count (rose/children rose))))))

(def pruneable-commands
  '({:command :take-key, :args [:user-a]}
    {:command :take-key, :args [:user-a]}
    {:command :unlock-door, :args [:user-a]}))

(deftest test-prune-commands
  (is (= [{:command :take-key, :args [:user-a]}
          {:command :unlock-door, :args [:user-a]}]
         (:commands
           (impl/prune-commands* state/model init-state pruneable-commands)))))

(deftest test-prune-before-after-meta
  (is (every?
        (fn [x]
          (let [m (meta x)]
            (and (contains? m :before)
                 (contains? m :after))))
        (:commands
          (impl/prune-commands* state/model init-state pruneable-commands)))))

(deftest test-prune-commands-before-after-meta
  (is (every?
        (fn [x]
          (let [m (meta x)]
            (and (contains? m :before)
                 (contains? m :after))))
        (:commands
          (impl/prune-commands* state/model init-state pruneable-commands)))))

(deftest test-commands-before-after-meta
  (is (every?
        (fn [x]
          (let [m (meta x)]
            (and (contains? m :before)
                 (contains? m :after))))
        (gen/generate (fugato/commands state/model init-state)))))

(comment
  (require '[clojure.pprint :refer [pprint]])

  (test/run-tests)

  (pprint
    (map rose/root
      (tree-seq (fn [x] (seq (rose/children x))) rose/children
        (impl/commands-rose state/model init-state commands 1))))

  )
