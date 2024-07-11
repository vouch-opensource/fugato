;; Copyright © 2024 Vouch.io LLC

(ns io.vouch.state-gen.impl
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.rose-tree :as rose]))

(def base-freq 256)

;; =============================================================================
;; Generator Stuff

(defn command-spec->gen
  "Given a command description, return a generator for that command."
  [spec state {:keys [command]}]
  (gen/hash-map
    :command (gen/return command)
    :args    ((get-in spec [command :args]) state)))

(defn freqs
  "Compute a likelihood for a command."
  [model state {:keys [command] :as new-command}]
  [(get-in model [command :freq] base-freq)
   (command-spec->gen model state new-command)])

(defn run?
  "Check that a command with the current state is runnable"
  [model state command]
  (if-let [run? (get-in model [command :run?])]
    (run? state)
    true))

(defn model->commands
  "Convert a command spec into a vector of commands for generation"
  [model state]
  (reduce
    (fn [acc command-name]
      (if (run? model state command-name)
        (conj acc {:command command-name})
        acc))
    [] (keys model)))

(defn all-drop1
  "Lazy returns all versions of xs minus one element"
  [xs]
  (map
    (fn [i]
      (concat (take i xs) (drop (inc i) xs)))
    (range (count xs))))

(defn prune-commands*
  [model init-state commands]
  (reduce
    (fn [ret command]
      (let [{:keys [valid? next-state] :as command-spec} (get model (:command command))]
        (assert (some? command-spec)
          (str "Command spec " command-spec " does not exist"))
        (cond
          (and valid? (valid? (:state ret) command))
          (-> ret
            (update :state #(next-state % command))
            (update :commands conj command))

          (not valid?)
          (-> ret
            (update :state #(next-state % command))
            (update :commands conj command))

          :else ret)))
    {:state init-state :commands []} commands))

(defn prune-commands
  "Given a spec and init-state prune commands that are invalid via :valid?
  conditions."
  [model init-state commands]
  (:commands (prune-commands* model init-state commands)))

(defn command-gen
  [model state]
  ;; should this be configurable? per-step - a la maximization PropEr
  ;; needs to be evaluated
  (gen/frequency
    (into [] (map #(freqs model state %))
      (model->commands model state))))

(defn commands-rose [model init-state commands]
  (rose/make-rose commands
    (map
      (comp
        #(commands-rose model init-state %)
        #(prune-commands model init-state %))
      (all-drop1 commands))))

(defn commands
  ([model state num-elements]
   (if (zero? num-elements)
     (gen/return '())
     (gen/gen-bind (command-gen model state)
       (fn [rose]
         (let [command (rose/root rose)
               state'  ((get-in model [(:command command) :next-state]) state command)]
           (gen/gen-fmap
             (fn [rose]
               (commands-rose model state (conj (rose/root rose) command)))
             (commands model state' (dec num-elements)))))))))
