# clj-miniconcat

`clj-miniconcat` is a toy [concatenative language](http://concatenative.org)
implemented in [Clojure](http://www.clojure.org/).  What inspired me to toy
around was
[this excellent blog post by Jon Purdy](http://evincarofautumn.blogspot.com/2012/02/why-concatenative-programming-matters.html).
The project uses [Leiningen](https://github.com/technomancy/leiningen) for
dependency management, building, and test automation.

## Usage

After cloning this repository, fire up a clojure REPL.

```
$ cd ~/path/to/clj-miniconcat
$ lein repl
user> (use 'miniconcat.core)
nil
```

You are ready to go.

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

Define doubling and tripling words, and apply tripling on 3 followed by
doubling the result.

```
user> (run-concat
       'double 1 [2 :*]            :define-word  ;; doubling is multiplying by 2
       'triple 1 [:dup :double :+] :define-word  ;; tripling is doubling and adding once again

       3 :triple :double)
18
```

Of course, there's also the branching word `:if` which consumes the three
top-most items from the stack: the condition, a then quotation, and an else
quotation.  The clojure rule of truthiness applies: `false` and `nil` are
falsy, everything else is truthy.

```
user> (run-concat 2 :neg? ["Math is broken"] ["Math still works"] :if)
"Math still works"
```

Combined with recursion, that's all you need for anything, I think.  So here's
a definition of the factorial function including its application on the number
20.

```
user=> (run-concat
        'fact 1                                 ;; define the factorial as word
        [:dup
         :zero?
         [:ignore 1]
         [:dup :dec :fact :*] :if] :define-word

        20 :fact)                               ;; calculate the factorial of 20
2432902008176640000
```

That version is pretty much the usual recursive definition of the faculty.  It
will pop all integers from 20 to 0 onto the stack followed by a cascade of `:*`
words, replace the 0 with a 1 (the termination criterion), and then start
multiplying.  That's hammering the stack quite a bit, and as a result, it's
about 40 times slower than the idiomatic clojure version `(reduce * (range 0
21))`.

But, hey, we can implement our factorial pretty similar.

```
user> (run-concat
       'fact 1 [:inc 1 :swap :range2 * :swap :reduce1] :define-word
       20 :fact)
2432902008176640000
```

The `:range2` is just the clojure `range` function with start (including) and
end (excluding, thus we `:inc`) value.  Since `:range2` wants its start value
before the end value, we have to `:swap` the 2 top-most stack items.  Finally,
we `:reduce1` the range using `*`, the plain clojure multiplication.
`:reduce1` is `reduce` without start value.  Again we have to `:swap`, because
`:reduce1` wants the reduction function and then the seq, not the other way
round.  Note that we can use plain clojure functions as a kind of `quotation`.

Now that version is only about factor 2 slower than the plain clojure version.

## License

Copyright (C) 2012 Tassilo Horn <tassilo@member.fsf.org>

Distributed under the Eclipse Public License, the same as Clojure.
