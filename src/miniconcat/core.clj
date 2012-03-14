(ns miniconcat.core)

;;# Stack & Words

(def ^{:dynamic true :private true}
  *stack* nil)
(def ^:private
  +words+ {})

;;# Utilities

(defn push! [& args]
  (swap! *stack* into args))

;;# Registering words

(defn register-word [name argcount definition]
  (alter-var-root #'+words+ assoc name [argcount definition]))

;;# The core words

(register-word :dup         1 #(push! %1 %1))
(register-word :swap        2 #(push! %2 %1))
(register-word :if          3 (fn [condition then else]
                                (apply push! (if condition
                                               then
                                               else))))
(register-word :ignore      1 (fn [e]))
(register-word :print-top   0 #(println (first @*stack*)))
(register-word :print-stack 0 #(println @*stack*))


;;# Defining words from clojure functions

(defmacro register-words-for-clj-fns [word argcount fn & more]
  `(do
     (register-word ~word ~argcount
                    (fn [& args#]
                      (push! (apply ~fn args#))))
     ~(when (seq more)
        `(register-words-for-clj-fns
          ~(nth more 0) ~(nth more 1) ~(nth more 2)
          ~@(drop 3 more)))))

(register-words-for-clj-fns
 :+       2   +
 :-       2   -
 :*       2   *
 :div     2   /
 :pos?    1   pos?
 :zero?   1   zero?
 :neg?    1   neg?
 :inc     1   inc
 :dec     1   dec
 :filter  2   filter
 :reduce  3   reduce
 :reduce1 3   reduce
 :map1    2   map
 :map2    3   map
 :map3    4   map)

;;# The runtime

(defn- extract-args [n]
  (let [[r s] ((juxt take drop) n @*stack*)]
    (swap! *stack* (constantly s))
    (reverse r)))

(defn- concat-apply [a]
  (if-let [spec (+words+ a)]
    (apply (spec 1) (extract-args (spec 0)))
    (push! a)))

(defn- run-concat-1
  ([args & words]
     (run-concat-1 args)
     (run-concat-1 words))
  ([args]
     (doseq [a args]
       (concat-apply a))))

(defn run-concat [& args]
  (binding [*stack* (atom (list))]
    (run-concat-1 args)
    (first @*stack*)))

;;# Defining words in the concat language itself

(register-word :define-word 3
               (fn [name argcount definition]
                 (register-word name argcount
                                (fn [& args]
                                  (apply run-concat-1 args definition)))))
