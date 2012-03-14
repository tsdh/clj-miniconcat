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

### Defining own words

Using the function `register-word` you can define your own words.  It gets a
word name, the argument count, the definition of the word as a clojure
function, and a docstring.  For example, that's the definition of `:dup`.

```
(register-word :dup 1 #(push-stack! %1 %1)
               "Duplicates the top item:\n  (x :dup ...) -> (x x ...)")
```

This is a pretty bare-bones function manipulating the stack directly.  It's
mostly intended for implementing the basic words like `:dup`.

There are two other approaches to defining your own words.

1. Integrating clojure functions
2. Defining words in miniconcat itself

#### Integrating clojure fns as words

You can integrate plain clojure functions as words using the macro
`register-words-for-clj-fns` like so:

```
user> (register-words-for-clj-fns
       :frozzle  3 *  "Frozzles the three top-most numbers."
       :frozzle' 3 *' "Frozzles the three top-most numbers; promotes to bignums."
       ;; more quadruples
      )
user> (run-concat 1 2 3 :frozzle)
6
```

After that, `:frozzle` is a word that consumes the three top-most items from
the stack, applies the clojure function `+` to them (i.e., adds them), and
pushes the result back on the stack.

#### Defining words in miniconcat itself

You can define words in the language itself using the word `:define-word`.  It
consumes three items from the stack, namely the new word's name, its argument
count, and its definition as a quotation (a vector).

```
user> (run-concat 'flubble 3 [:+ :+] :define-word  ;; define the new word :flubble
       1 :dup 3 :flubble)                          ;; use the new word
5
```

Note that the new word's name has to be specified either as symbol or as
string, but not as a keyword (definition before use).

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
