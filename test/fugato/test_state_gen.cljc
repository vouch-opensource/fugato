;; Copyright © 2024 Vouch.io LLC

(ns fugato.test-state-gen
  (:require [clojure.test :as test :refer [deftest is]]
            [clojure.test.check.generators :as gen]
            [fugato.core :as fugato]))

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

(defn door-locked? [state]
  (= :locked (:door state)))

(defn door-closed? [state]
  (= :closed (:door state)))

(defn door-open? [state]
  (= :open (:door state)))

(defn some-user-with-key? [state]
  (contains? (into (:user-a state) (:user-b state)) :key))

(defn user-with-key [state]
  (cond
    (contains? (:user-a state) :key) :user-a
    (contains? (:user-b state) :key) :user-b))

(defn room-with-key [state]
  (cond
    (contains? (:room-1 state) :key) :room-1
    (contains? (:room-2 state) :key) :room-2))

(defn user-and-key-in-same-room? [state]
  (let [room (room-with-key state)]
    (> (count (get state room)) 1)))

(defn user->room [state user]
  (cond
    (contains? (:room-1 state) user) :room-1
    (contains? (:room-2 state) user) :room-2))

(defn door-closed-and-user-has-key? [state user]
  (and (door-closed? state)
       (contains? (get state user) :key)))

(defn door-locked-and-user-has-key? [state user]
  (and (door-locked? state)
       (contains? (get state user) :key)))

;; =============================================================================
;; The Model

(def open-spec
  {:freq       2
   :run?       (fn [state] (door-closed? state))
   :args       (fn [state] (gen/tuple (gen/elements [:user-a :user-b])))
   :next-state (fn [state command] (assoc state :door :open))
   :valid?     (fn [state command] (door-closed? state))})

(def close-spec
  {:run?       (fn [state] (door-open? state))
   :args       (fn [state] (gen/tuple (gen/elements [:user-a :user-b])))
   :next-state (fn [state command] (assoc state :door :closed))
   :valid?     (fn [state command] (door-open? state))})

(def lock-spec
  {:run?       (fn [state] (and (door-closed? state)
                                (some-user-with-key? state)))
   :args       (fn [state] (gen/tuple (gen/return (user-with-key state))))
   :next-state (fn [state command] (assoc state :door :locked))
   :valid?     (fn [state {[user] :args :as commmand}]
                 (door-closed-and-user-has-key? state user))})

(def unlock-spec
  {:freq       2
   :run?       (fn [state] (and (some-user-with-key? state)
                                (door-locked? state)))
   :args       (fn [state] (gen/tuple (gen/return (user-with-key state))))
   :next-state (fn [state _] (assoc state :door :closed))
   :valid?     (fn [state {[user] :args :as command}]
                 (door-locked-and-user-has-key? state user))})

(def take-key-spec
  {:run?       (fn [state] (user-and-key-in-same-room? state))
   :args       (fn [state]
                 (gen/tuple
                   (gen/elements
                     (disj (get state (room-with-key state)) :key))))
   :next-state (fn [state {[user] :args :as command}]
                 (-> state
                   (update user conj :key)
                   (update (user->room state user) disj :key)))
   :valid?     (fn [state {[user] :args :as command}]
                 (= (user->room state user)
                    (room-with-key state)))})

(def drop-key-spec
  {:run?       (fn [state] (some-user-with-key? state))
   :args       (fn [state] (gen/tuple (gen/return (user-with-key state))))
   :next-state (fn [state {[user] :args :as command}]
                 (-> state
                   (update user disj :key)
                   (update (user->room state user) conj :key)))
   :valid?     (fn [state {[user] :args}]
                 (= user (user-with-key state)))})

(def next-room
  {:room-1 :room-2
   :room-2 :room-1})

(def move-spec
  {:freq       2
   :run?       (fn [state] (= :open (:door state)))
   :args       (fn [state]
                 (gen/bind (gen/elements [:user-a :user-b])
                   (fn [user]
                     (gen/tuple
                       (gen/return user)
                       (gen/return (next-room (user->room state user)))))))
   :next-state (fn [state {[user] :args :as command}]
                 (let [prev-room (user->room state user)
                       room (prev-room next-room)]
                   (-> state
                     (update prev-room disj user)
                     (update room conj user))))
   :valid?     (fn [state command] (door-open? state))})

(def model
  {:open-door   open-spec
   :close-door  close-spec
   :lock-door   lock-spec
   :unlock-door unlock-spec
   :take-key    take-key-spec
   :drop-key    drop-key-spec
   :move        move-spec})

;; =============================================================================
;; Test the model

(deftest test-take-drop
  (let [state (-> init-state
                ((-> model :take-key :next-state) {:command :take-key :args [:user-a]})
                ((-> model :drop-key :next-state) {:command :drop-key :args [:user-a]}))]
    (is (= state init-state))))

(deftest test-execute
  (let [commands [{:command :take-key :args [:user-a]}
                  {:command :drop-key :args [:user-a]}]]
    (is (= init-state
           (fugato/execute model init-state commands)))))

(comment

  (test/run-tests)

  )
