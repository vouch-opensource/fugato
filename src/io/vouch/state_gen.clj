;; Copyright © 2024 Vouch.io LLC

(ns io.vouch.state-gen
  "Data oriented stateful generative testing library for Clojure with shrinking
   support.

  A `model` is a hash map with the following structure:

  {:command-name-a <CommandSpecMap>
   :command-name-b <CommandSpecMap> ...}

  A `command-spec` is a <CommandSpecMap> with the following structure:

  {:args       <Fn(State)->ArgsGenerator>
   :freq       <Integer>
   :next-state <Fn(State, Args)->State>
   :run?       <Fn(State)->Boolean>
   :valid?     <Fn(State)->Boolean>}

  A `state` is a user specified map to store state information so that the model
  can be used to create a sequence of commands where the generation of the
  commands depends on previously generated commands (because they changed the
  state)."
  (:require [clojure.test.check.generators :as gen]
            [io.vouch.state-gen.impl :as impl]))

(defn commands
  "Given a model and an initial state, generate a sequence of commands:

   [{:command command-name :args [...]} ...]
   "
  ([model init-state]
   (gen/bind
     (gen/sized #(gen/choose 0 %))
     (fn [num-elements]
       (impl/commands model init-state num-elements))))
  ([model init-state num-elements]
   (gen/bind
     (gen/choose 0 num-elements)
     (fn [num-elements]
       (impl/commands model init-state num-elements))))
  ([model init-state min-elements max-elements]
   (gen/bind
     (gen/choose min-elements max-elements)
     (fn [num-elements]
       (impl/commands model init-state num-elements)))))
