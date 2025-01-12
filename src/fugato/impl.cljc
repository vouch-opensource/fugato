;; Copyright © 2024 Vouch.io LLC

(ns fugato.impl
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.rose-tree :as rose]))

(def base-freq 1)

;; =============================================================================
;; Generator Stuff

(defn command-spec->gen
  "Given a command description, return a generator for that command."
  [spec state {:keys [command]}]
  (gen/hash-map
    :command (gen/return command)
    :args    ((get-in spec [command :args]
                (constantly (gen/return [])))
              state)))

(defn freqs
  "Compute a likelihood for a command."
  [model state {:keys [freq] :as new-command}]
  [freq (command-spec->gen model state new-command)])

(defn run-freq
  "Check that a command with the current state is runnable"
  [model state command]
  (if-let [run-freq (get-in model [command :run-freq])]
    (run-freq state)
    (if-let [run? (get-in model [command :run?])]
      (if (run? state) base-freq 0)
      base-freq)))

(defn model->commands
  "Convert a model into a vector of commands for generation"
  [model state]
  (reduce
   (fn [acc command-name]
     (let [freq (run-freq model state command-name)]
       (if (pos? freq)
         (conj acc {:command command-name :freq freq})
         acc)))
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
    (fn [{:keys [state] :as ret} command]
      (let [{:keys [valid? next-state] :as command-spec} (get model (:command command))]
        (assert (some? command-spec)
          (str "Command spec " command-spec " does not exist"))
        (cond
          ;; either nothing to check or we passed
          (or (not valid?) (valid? state command))
          (let [state' (next-state state command)]
            (-> ret
              (assoc :state state')
              (update :commands conj (with-meta command {:before state :after state'}))))

          ;; skip
          :else ret)))
    {:state init-state :commands []} commands))

(defn prune-commands
  "Given a model and init-state, prune commands that are invalid via :valid?
  conditions."
  [model init-state commands]
  (:commands (prune-commands* model init-state commands)))

(defn command-gen
  [model state]
  ;; should this be configurable? per-step - a la maximization PropEr
  ;; needs to be evaluated
  (let [commands (model->commands model state)]
    (if (seq commands)
      (gen/frequency (mapv #(freqs model state %) commands))
      (gen/return nil))))

(defn commands-rose
  [model init-state commands min-elements]
  (when (seq commands)
    (rose/make-rose
     commands
     (when (> (count commands) min-elements)
       (remove nil?
               (map
                (comp
                 #(commands-rose model init-state % min-elements)
                 #(prune-commands model init-state %))
                (all-drop1 commands)))))))

(defn commands
  ([model state num-elements]
   (commands model state num-elements 0))
  ([model state num-elements min-elements]
   (if (zero? num-elements)
     (gen/return '())
     (gen/gen-bind (command-gen model state)
       (fn [rose]
         (if-let [command (rose/root rose)]
           (let [state' ((get-in model [(:command command) :next-state]) state command)]
             (gen/gen-fmap
               (fn [rose]
                 (commands-rose model state
                   (conj (rose/root rose)
                         (with-meta command {:before (with-meta state nil)
                                             :after  (with-meta state' nil)}))
                   min-elements))
               (commands model
                         (with-meta state' {:previous-command command})
                         (dec num-elements)
                         min-elements)))
           (gen/return '())))))))
