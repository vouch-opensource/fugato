;; Copyright © 2018 Vouch.io LLC

(ns io.vouch.state-gen
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.rose-tree :as rose]))

(def base-freq 256)

;; =============================================================================
;; Debug Helpers

(defn seq-rose
  "Convert a rose tree into a printable list for visual debugging."
  [rose]
  (list (rose/root rose) (map seq-rose (rose/children rose))))

;; =============================================================================
;; Generator Stuff

(defn command-desc->gen
  "Given a command description, return a generator for that command."
  [spec state {:keys [command]}]
  (gen/hash-map
    :command (gen/return command)
    :args    ((get-in spec [command :args]) state)))

(defn freqs
  "Compute a likelihood for a command."
  [spec state {:keys [command] :as new-command}]
  [(get-in spec [command :freq] base-freq)
   (command-desc->gen spec state new-command)])

(defn run?
  "Check that a command with the current state is runnable"
  [spec state command]
  (if-let [run? (get-in spec [command :run?])]
    (run? state)
    true))

(defn spec->commands
  "Convert a command spec into a vector of commands for generation"
  [spec state]
  (reduce
    (fn [acc command-name]
      (if (run? spec state command-name)
        (conj acc {:command command-name})
        acc))
    [] (keys spec)))

(defn all-drop1
  "Lazy returns all versions of xs minus one element"
  [xs]
  (map
    (fn [i]
      (concat (take i xs) (drop (inc i) xs)))
    (range (count xs))))

(defn prune-commands*
  [spec init-state commands]
  (reduce
    (fn [ret command]
      (let [{:keys [valid? next-state] :as command-spec} (get spec (:command command))]
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
  [spec init-state commands]
  (:commands (prune-commands* spec init-state commands)))

(defn command-gen
  [spec state]
  ;; should this be configurable? per-step - a la maximization PropEr
  ;; needs to be evaluated
  (gen/frequency
    (into [] (map #(freqs spec state %))
      (spec->commands spec state))))

(defn commands-rose [spec init-state commands]
  (rose/make-rose commands
    (map
      (comp
        #(commands-rose spec init-state %)
        #(prune-commands spec init-state %))
      (all-drop1 commands))))

(defn commands*
  ([spec state num-elements]
   (if (zero? num-elements)
     (gen/return '())
     (gen/gen-bind (command-gen spec state)
       (fn [rose]
         (let [command (rose/root rose)
               state'  ((get-in spec [(:command command) :next-state]) state command)]
           (gen/gen-fmap
             (fn [rose]
               (commands-rose spec state (conj (rose/root rose) command)))
             (commands* spec state' (dec num-elements)))))))))

(defn commands
  "Given a command spec and a initial state, produce generate commands.
   The result will be of the form:

   [{:command command-name :args [...]} ...]
   "
  ([spec init-state]
   (gen/bind
     (gen/sized #(gen/choose 0 %))
     (fn [num-elements]
       (commands* spec init-state num-elements))))
  ([spec init-state num-elements]
   (gen/bind
     (gen/choose 0 num-elements)
     (fn [num-elements]
       (commands* spec init-state num-elements))))
  ([spec init-state min-elements max-elements]
   (gen/bind
     (gen/choose min-elements max-elements)
     (fn [num-elements]
       (commands* spec init-state num-elements)))))
