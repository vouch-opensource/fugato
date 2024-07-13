;; Copyright © 2024 Vouch.io LLC

(ns io.vouch.test-room-world
  (:require [clojure.test :as test :refer [deftest is]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [io.vouch.state-gen :as state-gen]
            [io.vouch.test-state-gen :as test.state-gen :refer [model]]))

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
  (= :closed (:door world)))

(defn user->room [world user]
  (if (contains? (:room-1 world) user)
    :room-1
    :room-2))

;; =============================================================================
;; Actions

(def next-room
  {:room-1 :room-2
   :room-2 :room-1})

(defn move [world user]
  (let [prev-room (user->room world user)
        room      (prev-room next-room)]
    (cond-> world
      (door-open? world)
      (->
        (update room conj user)
        (update prev-room disj user)))))

(defn open [world user]
  (cond-> world
    (not (door-locked? world))
    (assoc :door :open)))

(defn close [world user]
  (cond-> world
    (door-open? world)
    (assoc :door :closed)))

(defn lock [world user]
  (cond-> world
    (and (door-closed? world)
         (has-key? world user))
    (assoc :door :locked)))

(defn unlock [world user]
  (cond-> world
    (and (door-locked? world)
         (has-key? world user))
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
  (is (has-key? world :room-1))
  (is (not (has-key? world :room-2)))
  (is (not (has-key? world :user-a)))
  (is (not (has-key? world :user-b))))

(deftest test-take-key
  (let [world' (take-key world :user-a)]
    (is (has-key? world' :user-a))))

(deftest test-drop-key
  (let [world' (-> world
                 (take-key :user-a)
                 (drop-key :user-a))]
    (is (true? (has-key? world' :room-1)))))

(deftest test-take-key-wrong-room
  (let [world' (take-key world :user-b)]
    (is (not (has-key? world' :user-b)))))

(deftest test-unlock
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock :user-a))]
    (is (not (door-locked? world')))))

(deftest test-unlock-without-key
  (let [world' (unlock world :user-a)]
    (is (door-locked? world'))))

(deftest test-lock
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock :user-a)
                 (lock :user-a))]
    (is (door-locked? world'))))

(deftest test-move
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock :user-a)
                 (open :user-a)
                 (move :user-a))]
    (is (= :room-2 (user->room world' :user-a)))))

(deftest test-close
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock :user-a)
                 (open :user-a)
                 (close :user-a))]
    (is (door-closed? world'))))

;; =============================================================================
;; Property Tests

(def command->fn
  {:open     open
   :close    close
   :lock     lock
   :unlock   unlock
   :take-key take-key
   :drop-key drop-key
   :move     move})

(defn run [state commands]
  (reduce
    (fn [state {:keys [command args]}]
      (apply (get command->fn command) state args))
    state commands))

(defspec model-eq-reality 10
  (prop/for-all [commands (state-gen/commands model world 1 20)]
    (if (seq commands)
      (= (run world commands) (-> commands last meta :after))
      true)))

(comment

  (test/run-tests)

  (let [xs     (last (gen/sample (state-gen/commands model world 10 20) 10))
        world' (run world xs)]
    (println world' (-> xs last meta :after)))

  )
