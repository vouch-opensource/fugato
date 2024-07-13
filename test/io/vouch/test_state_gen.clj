;; Copyright © 2024 Vouch.io LLC

(ns io.vouch.test-state-gen
  (:require [clojure.test.check.generators :as gen]
            [io.vouch.state-gen :as state-gen]
            [io.vouch.state-gen.impl :as state-gen.impl]))

;; =============================================================================
;; The State

(def init-state
  {:user-a  #{}
   :user-b  #{}
   :door    :locked
   :room-1  #{:key :user-a :user-b}
   :room-2  #{}})

;; =============================================================================
;; Helpers

(defn some-user-with-key? [state]
  (contains? (into (:user-a state) (:user-b state)) :key))

(defn user-with-key [state]
  (cond
    (contains? (:user-a state) :key) :user-a
    (contains? (:user-b state) :key) :user-b))

(defn dropped-key? [state]
  (contains? (into (:room-1 state) (:room-2 state)) :key))

(defn room-with-key [state]
  (cond
    (contains? (:room-1 state) :key) :room-1
    (contains? (:room-2 state) :key) :room-2))

(defn user->room [state user]
  (cond
    (contains? (:room-1 state) user) :room-1
    (contains? (:room-2 state) user) :room-2))

;; =============================================================================
;; The Model

(def open-spec
  {:run?       (fn [state] (= :closed (:door state)))
   :args       (fn [state] (gen/tuple (gen/elements [:user-a :user-b])))
   :next-state (fn [state command] (assoc state :door :open))})

(def close-spec
  {:run?       (fn [state] (= :open (:door state)))
   :args       (fn [state] (gen/tuple (gen/elements [:user-a :user-b])))
   :next-state (fn [state command] (assoc state :door :closed))})

(def lock-spec
  {:run?       (fn [state] (and (some-user-with-key? state)
                             (= :closed (:door state))))
   :args       (fn [state] (gen/tuple (gen/return (user-with-key state))))
   :next-state (fn [state _] (assoc state :door :locked))})

(def unlock-spec
  {:run?       (fn [state] (and (some-user-with-key? state)
                                (= :locked (:door state))))
   :args       (fn [state] (gen/tuple (gen/return (user-with-key state))))
   :next-state (fn [state _] (assoc state :door :unlocked))})

(def take-key-spec
  {:run?       (fn [state]
                 (dropped-key? state))
   :args       (fn [state]
                 (gen/tuple
                   (gen/elements
                     (disj (get state (room-with-key state)) :key))))
   :next-state (fn [state {:keys [args]}]
                 (let [user (first args)]
                   (-> state
                     (update user conj :key)
                     (update (user->room state user) disj :key))))})

(def drop-key-spec
  {:run?       (fn [state] (some-user-with-key? state))
   :args       (fn [state] (gen/tuple (gen/return (user-with-key state))))
   :next-state (fn [state {:keys [args]}]
                 (let [user (first args)]
                   (-> state
                     (update user disj :key)
                     (update (user->room state user) conj :key))))})

(def next-room
  {:room-1 :room-2
   :room-2 :room-1})

(def move-spec
  {:run?       (fn [state] (= :open (:door state)))
   :args       (fn [state] (gen/tuple (gen/elements [:user-a :user-b])))
   :next-state (fn [state {:keys [args]}]
                 (let [user (first args)
                       prev-room (user->room state user)
                       room (prev-room next-room)]
                   (-> state
                     (update prev-room disj user)
                     (update room conj user))))})

(def model
  {:open      open-spec
   :close     close-spec
   :lock      lock-spec
   :unlock    unlock-spec
   :take-key  take-key-spec
   :move      move-spec})

(comment

  (require '[clojure.pprint :refer [pprint]])
  (pprint (last (gen/sample (state-gen/commands model init-state 3) 10)))
  (pprint (last (gen/sample (state-gen/commands model init-state 10) 10)))

  ;; checking some things
  (state-gen.impl/model->commands model init-state)
  (state-gen.impl/model->commands model
    (-> init-state
      (update :user-a conj :key)
      (update :room-1 disj :key)))

  ;; this seems ok
  (into [] (map #(state-gen.impl/freqs model init-state %))
    (state-gen.impl/model->commands model init-state))

  )
