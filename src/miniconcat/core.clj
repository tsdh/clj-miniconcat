(ns miniconcat.core)

;;# Stack & Words

(def ^{:dynamic true :private true :tag java.util.Stack}
  *stack* nil)
(def ^:private
  +words+ {})

;;# Utilities

(defn push-stack!
  ([& args]
     (doseq [a (reverse args)]
       (.push *stack* a))))

(defn pop-stack! []
  (.pop *stack*))

;;# Registering words

(defn register-word
  "Registers a new miniconcat word named `name` (a keyword) consuming
  `argcount` arguments and the given `docstring`.  `definition` is a function
  with `argcount` args that implements the behavior."
  [name argcount definition docstring]
  (alter-var-root #'+words+ assoc name [argcount definition docstring]))

;;# The core words

(register-word :dup 1 #(push-stack! %1 %1)
               "Duplicates the top item:\n  (x :dup ...) -> (x x ...)")
(register-word :swap        2 #(push-stack! %2 %1)
               "Swaps the two top-most items:\n  (x y :swap ...) -> (y x ...)")
(register-word :if          3 (fn [condition then else]
                                (apply push-stack! (if condition
                                                     then
                                                     else)))
               "The if control structure:
  (.. condition [then-quot] [else-quot] :if ...)
    -truthy-condition-> (.. then-quot ...)
    -falsy--condition-> (.. else-quot ...)")
(register-word :ignore      1 (fn [e])
               "Consumes one item doing nothing:\n  (x :ignore ...) -> (...)")


;;# Defining words from clojure functions

(defmacro register-words-for-clj-fns [word argcount fn docstr & more]
  `(do
     (register-word ~word ~argcount
                    (fn [& args#]
                      (push-stack! (apply ~fn args#)))
                    ~docstr)
     ~(when (seq more)
        `(register-words-for-clj-fns
          ~(nth more 0) ~(nth more 1) ~(nth more 2) ~(nth more 3)
          ~@(drop 4 more)))))

(register-words-for-clj-fns
 :+       2   +       "Adds the two top-most numbers:\n  (1 2 :+ ...) -> (3 ...)"
 :+'      2   +'      "Adds the two top-most numbers; promotes to bignums:\n  (1 2 :+' ...) -> (3 ...)"
 :-       2   -       "Substracts the two top-most numbers:\n  (1 2 :- ...) -> (-1 ...)"
 :-'      2   -'      "Substracts the two top-most numbers; promotes to bignums:\n  (1 2 :-' ...) -> (-1 ...)"
 :*       2   *       "Multiplies the two top-most numbers:\n  (2 3 :* ...) -> (6 ...)"
 :*'      2   *'      "Multiplies the two top-most numbers; promotes to bignums:\n  (2 3 :*' ...) -> (6 ...)"
 :div     2   /       "Divides the two top-most numbers:\n  (2 3 :div ...) -> (2/3 ...)"
 :pos?    1   pos?    "Tests if the top item is positive:\n  (1 :pos? ...) -> (true ...)"
 :zero?   1   zero?   "Tests if the top item is zero:\n  (0.0 :zero? ...) -> (true ...)"
 :neg?    1   neg?    "Tests if the top item is negative:\n  (1 :neg? ...) -> (false ...)"
 :inc     1   inc     "Increments the top item:\n  (10 :inc ...) -> (11 ...)"
 :dec     1   dec     "Decrements the top item:\n  (10 :dec ...) -> (9 ...)"
 :filter  2   filter  "Filters a seq by a predicate:\n  (even? [1 2 3 4] :filter ...) -> ((2 4) ...)"
 :reduce  3   reduce  "Reduces with start value:\n  (* 1 [2 3 4] :reduce ...) -> (24 ...)"
 :reduce1 2   reduce  "Reduces without start value:\n  (+ [2 3 4] :reduce ...) -> (6 ...)"
 :map     2   map     "Maps a function thru one seq:\n  (inc [1 2 3] :map ...) -> ((2 3 4) ...)"
 :map2    3   map     "Maps a function thru two seqs:\n  (+ [1 2 3] [3 2 1] :map2 ...) -> ((4 4 4) ...)"
 :map3    4   map     "Maps a function thru three seqs:\n  (+ [1 2 3] [3 2 1] [1 2 3] :map3 ...) -> ((5 6 7) ...)")

;;# The runtime

(defn- extract-args [n v]
  (let [idx (- (count v) n)]
    [(subvec v idx) (subvec v 0 idx)]))

(defn substitute! []
  (loop [r []]
    (if (.empty *stack*)
      (first r)
      (let [x (pop-stack!)]
        (if-let [spec (+words+ x)]
          (let [[args other] (extract-args (spec 0) r)]
            (apply (spec 1) args)
            (apply push-stack! other)
            (recur []))
          (if (keyword? x)
            (throw (RuntimeException. (str "Undefined word `" x "`.")))
            (recur (conj r x))))))))

(defn run-concat [& args]
  (binding [*stack* (java.util.Stack.)]
    (apply push-stack! args)
    (substitute!)))

;;# Defining words in the concat language itself

(register-word :define-word 3
               (fn [name argcount definition]
                 (register-word (keyword name) argcount
                                (fn [& args]
                                  (apply push-stack! definition)
                                  (apply push-stack! args))
                                "(dynamically defined word)"))
               "Defines a new word.")

;;# Documentation

(defn list-words
  "Lists the currently defined words."
  []
  (keys +words+))

(defn doc-word
  "Show the documentation for word `w`, a keyword."
  [w]
  (when-not (keyword? w)
    (throw (RuntimeException. "Words are keywords.")))
  (println "-------------------------")
  (if-let [spec (+words+ w)]
    (do
      (println "Word:" w)
      (println "Argcount:" (spec 0))
      (println (spec 2)))
    (println "No such word" w)))

(defn find-word-doc
  "Shows the docs for all words that match the regexp `re` or whose docstring
  matches `re`."
  [re]
  (when-not (instance? java.util.regex.Pattern re)
    (throw (RuntimeException. "find-word-doc gets a regexp.")))
  (doseq [[w spec] +words+]
    (when (or (re-matches re (name w))
              (re-matches re (spec 2)))
      (doc-word w))))
