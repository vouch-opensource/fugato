;; Copyright © 2024 Vouch.io LLC

(ns fugato.core
  "Data oriented stateful generative testing library for Clojure with shrinking
   support.

  A `model` is a hash map with the following structure:

  {:command-name-a <CommandSpecMap>
   :command-name-b <CommandSpecMap> ...}

  A `command-spec` is a <CommandSpecMap> with the following structure:

  {:args       <Fn(State)->ArgsGenerator>
   :freq       <Integer>
   :next-state <Fn(State, CommandMap)->State>
   :run?       <Fn(State)->Boolean>
   :valid?     <Fn(State, CommandMap)->Boolean>}

  A `command` has the following structure:

  {:command :a-name :args [...]}

  It is not strictly necessary for `:args` to be vector but recommended.

  A `state` is a user specified map to store state information so that the model
  can be used to create a sequence of commands where the generation of the
  commands depends on previously generated commands (because they changed the
  state)."
  (:require [clojure.test.check.generators :as gen]
            [fugato.impl :as impl]))

(defn commands
  "Given a model and an initial state, generate a sequence of commands:

   [{:command command-name :args [...]} ...]

   Optionally can request a maximum number of elements to return, or a minimum
   and maximum.
   "
  ([model init-state]
   (gen/bind
     (gen/sized #(gen/choose 0 %))
     (fn [num-elements]
       (impl/commands model init-state num-elements))))
  ([model init-state max-elements]
   (gen/bind
     (gen/choose 0 max-elements)
     (fn [num-elements]
       (impl/commands model init-state num-elements))))
  ([model init-state min-elements max-elements]
   (gen/bind
     (gen/choose min-elements max-elements)
     (fn [num-elements]
       (impl/commands model init-state num-elements)))))
