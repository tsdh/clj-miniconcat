# clj-miniconcat

`clj-miniconcat` is a toy [concatenative language](http://concatenative.org)
implemented in [Clojure](http://www.clojure.org/).  What inspired me to toy
around was
[this excellent blog post by Jon Purdy](http://evincarofautumn.blogspot.com/2012/02/why-concatenative-programming-matters.html).

## Usage

The main entry point to running concatenative programs is the function
`run-concat`.  It gets a concatenative program, runs it, and returns its
result.

Concatenative programs consist of words (functions represented as keywords) and
data (everything else).  A program is specified in postfix notation.  For
example, this program substracts 10 from 2.

```
user> (run-concat 2 10 :-)
-8
```

### Getting Documentation

You can list all available words with the function `list-words`.

```
user> (list-words)
(:dup :+ :zero? :* :- :neg? :map :+' :div :define-word :swap :inc :filter
 :reduce1 :reduce :*' :map2 :pos? :map3 :dec :if :ignore :-')
```

To get the documentation of a word, use the function `doc-word`.

```
user> (doc-word :dup)
-------------------------
Word: :dup
Argcount: 1
Duplicates the top item:
  (x :dup ...) -> (x x ...)
nil
```

To search the available words and their documentation, use `find-word-doc` with
a regular expression.

```
user> (find-word-doc #".*reduce.*")
-------------------------
Word: :reduce1
Argcount: 2
Reduces without start value:
  (+ [2 3 4] :reduce ...) -> (6 ...)
-------------------------
Word: :reduce
Argcount: 3
Reduces with start value:
  (* 1 [2 3 4] :reduce ...) -> (24 ...)
nil
```

### Examples

As an example, this is a definition of the factorial function (defined in the
miniconcat language itself) including its application on the number 20.

```
user=> (run-concat
         'fact 1                                 ;; name & argcount
         [:dup                                   ;; definition
          :zero?                                 ;; condition of :if
          [:ignore 1]                            ;; then-branch
          [:dup :dec :fact :*] :if] :define-word ;; else-branch, :if, and define the definition as new word :fact

         20 :fact)                               ;; calculate the factorial of 20
2432902008176640000
```

## License

Copyright (C) 2012 Tassilo Horn <tassilo@member.fsf.org>

Distributed under the Eclipse Public License, the same as Clojure.
