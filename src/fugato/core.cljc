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

   Can request a minimum number of elements to start quickly with interesting
   sequences. Can request min-shrink to avoid testing no commands or two few
   commands."
  ([model init-state]
   (gen/bind
     (gen/sized #(gen/choose 0 %))
     (fn [num-elements]
       (impl/commands model init-state num-elements))))
  ([model init-state min-elements]
   (commands model init-state min-elements 0))
  ([model init-state min-elements min-shrink]
   (gen/bind
     (gen/sized #(gen/choose min-elements (+ min-elements %)))
     (fn [num-elements]
       (impl/commands model init-state num-elements min-shrink)))))

(defn execute
  "Given a model, an initial state, and a sequence of commands execute them
  "
  ([model init-state commands]
   (reduce
     (fn [state {:keys [command] :as the-command}]
       (let [command-spec (get model command)]
         (if command-spec
           ((:next-state command-spec) state the-command)
           (throw (ex-info (str "Unknown command" command) the-command)))))
     init-state commands)))
