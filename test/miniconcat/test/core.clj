(ns miniconcat.test.core
  (:use [miniconcat.core])
  (:use [clojure.test]))

(deftest test-calc
  ;; 1 - 4 = -3
  (is (== -3 (run-concat 1 4 :-))))

(deftest test-map-reduce
  ;; map
  (is (= [2 4 6] (run-concat #(* 2 %1) [1 2 3] :map)))
  (is (= [2 4 6] (run-concat [1 2 3] #(* 2 %1) :swap :map)))
  ;; reduce
  (is (== 6 (run-concat + [1 2 3] :reduce1)))
  (is (== 6 (run-concat + 0 [1 2 3] :reduce))))

(deftest test-if
  (is (= "yay" (run-concat true  ["yay"] ["nay"] :if)))
  (is (= "nay" (run-concat false ["yay"] ["nay"] :if)))

  (is (= "yay" (run-concat 1 :pos?   ["yay"] ["nay"] :if)))
  (is (= "nay" (run-concat 1 :neg?   ["yay"] ["nay"] :if)))
  (is (= "nay" (run-concat 1 :zero?  ["yay"] ["nay"] :if)))

  (is (== 0 (run-concat 1 :dup :pos? [:dec] [:inc] :if))))

(deftest test-fact
  (println "test-fact")
  (is (== 2432902008176640000
          (run-concat
           'fact 1
           [:dup
            :zero?
            [:ignore 1]
            [:dup :dec :fact :*] :if] :define-word

           20 :fact))))
