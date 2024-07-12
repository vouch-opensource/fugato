;; Copyright © 2024 Vouch.io LLC

(ns io.vouch.test-room-world
  (:require [clojure.test :as test :refer [deftest is]]))

(def world
  {:user-a #{}
   :user-b #{}
   :door   :locked ;; #{:locked :closed :open}
   :room-1 #{:user-a :key}
   :room-2 #{:user-b}})

;; =============================================================================
;; Predicates & Helpers

(defn has-key? [world x]
  (boolean (some #{:key} (get world x))))

(defn door-locked? [world]
  (= :locked (:door world)))

(defn door-open? [world]
  (= :open (:door world)))

(defn door-closed? [world]
  (= :closed (:or world)))

(defn user->room [world user]
  (if (contains? (:room-1 world) user)
    :room-1
    :room-2))

;; =============================================================================
;; Actions

(defn move [world user room]
  (let [prev-room (user->room world user)]
    (cond-> world
      (door-open? world)
      (->
        (update room conj user)
        (update prev-room disj user)))))

(defn open [world user]
  (cond-> world
    (and (has-key? world user)
         (not (door-locked? world)))
    (assoc :door :open)))

(defn close [world user]
  (cond-> world
    (door-open? world)
    (assoc :door :open)))

(defn lock [world user]
  (cond-> world
    (door-closed? world)
    (assoc :door :locked)))

(defn unlock [world user]
  (cond-> world
    (door-locked? world)
    (assoc :door :closed)))

(defn drop-key [world user]
  (cond-> world
    (has-key? world user)
    (->
      (update user disj :key)
      (update (user->room world user) conj :key))))

(defn take-key [world user]
  (let [room (user->room world user)]
   (cond-> world
     (has-key? world room)
     (->
       (update user conj :key)
       (update room disj :key)))))

;; =============================================================================
;; Unit Tests

(deftest test-has-key?
  (is (true? (has-key? world :room-1)))
  (is (false? (has-key? world :room-2)))
  (is (false? (has-key? world :user-a)))
  (is (false? (has-key? world :user-b))))

(deftest test-take-key
  (let [world' (take-key world :user-a)]
    (is (true? (has-key? world' :user-a)))))

(deftest test-drop-key
  (let [world' (-> world
                 (take-key :user-a)
                 (drop-key :user-a))]
    (is (true? (has-key? world' :room-1)))))

(deftest test-take-key-wrong-room
  (let [world' (take-key world :user-b)]
    (is (false? (has-key? world' :user-b)))))

(comment

  (test/run-tests)

  )
