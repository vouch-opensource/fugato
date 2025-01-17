;; Copyright © 2024 Vouch.io LLC

(ns fugato.test-room-world
  (:require [clojure.test :as test :refer [deftest is testing]]
            [clojure.test.check :as test.check]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [fugato.core :as fugato]
            [fugato.test-state-gen :as fugato.state-gen :refer [model]]))

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

(defn move [world user room]
  (let [prev-room (user->room world user)
        room      (prev-room next-room)]
    (cond-> world
      (door-open? world)
      (->
        (update room conj user)
        (update prev-room disj user)))))

(defn open-door [world user]
  (cond-> world
    (not (door-locked? world))
    (assoc :door :open)))

(defn close-door [world user]
  (cond-> world
    (door-open? world)
    (assoc :door :closed)))

(defn lock-door [world user]
  (cond-> world
    (and (door-closed? world)
         (has-key? world user))
    (assoc :door :locked)))

(defn unlock-door [world user]
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
                 (unlock-door :user-a))]
    (is (not (door-locked? world')))))

(deftest test-unlock-without-key
  (let [world' (unlock-door world :user-a)]
    (is (door-locked? world'))))

(deftest test-lock
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock-door :user-a)
                 (lock-door :user-a))]
    (is (door-locked? world'))))

(deftest test-move
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock-door :user-a)
                 (open-door :user-a)
                 (move :user-a :room-2))]
    (is (= :room-2 (user->room world' :user-a)))))

(deftest test-close
  (let [world' (-> world
                 (take-key :user-a)
                 (unlock-door :user-a)
                 (open-door :user-a)
                 (close-door :user-a))]
    (is (door-closed? world'))))

;; =============================================================================
;; Property Tests

(def command->fn
  {:open-door   #'open-door
   :close-door  #'close-door
   :lock-door   #'lock-door
   :unlock-door #'unlock-door
   :take-key    #'take-key
   :drop-key    #'drop-key
   :move        #'move})

(defn run [state commands]
  (reduce
    (fn [state {:keys [command args]}]
      (if-let [fn (get command->fn command)]
        (apply fn state args)
        (throw
          (ex-info (str "Unknown command: " command ", args:" args)
            {:command command :arg args}))))
    state commands))

(def state-eq
  (prop/for-all [commands (fugato/commands model world 10 1)]
    (= (run world commands) (-> commands last meta :after))))

(defspec model-eq-reality 10 state-eq)

(defn broken-lock-door [world user]
  (cond-> world
    (and (door-closed? world)
         (has-key? world user))
    world #_(assoc :door :locked)))

(defmacro with-broken-lock-door [& body]
  (with-meta
    `(with-redefs [lock-door broken-lock-door]
       ~@body)
    (meta &form)))

(def broken-state-eq
  (prop/for-all [commands (fugato/commands model world 10 1)]
    (= (with-broken-lock-door (run world commands))
       (-> commands last meta :after))))

(deftest broken-model-eq-reality

  (testing

    "Buggy state transitions in the \"real\" implementation are exposed in
     generative tests against the model with minimal error cases after shrinking."

    (let [{:keys [pass?
                  shrunk]}
          (test.check/quick-check 25 broken-state-eq)

          {:keys [smallest]} shrunk]

      (is (false? pass?))
      ;; Initially :user-a and :key are both in :room-1 and the door is :locked;
      ;; to expose the bug, :user-a must pick up the key, unlock the door so
      ;; that the action of locking it again becomes accessible, and then
      ;; actually try to lock it again.
      (is (= [:take-key :unlock-door :lock-door]
             (map :command (first smallest)))))))

(comment

  (require '[clojure.pprint :refer [pprint]])

  (test/run-tests)

  ;; just verifying that we can reuse the seed
  (test.check/quick-check 10 state-eq
    :seed 1721227011247)

  (pprint (gen/generate (fugato/commands model world)))



  )
