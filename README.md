# clj-miniconcat

`clj-miniconcat` is a toy [concatenative language](http://concatenative.org)
implemented in [Clojure](http://www.clojure.org/).  What inspired me to toy
around was
[this excellent blog post by Jon Purdy](http://evincarofautumn.blogspot.com/2012/02/why-concatenative-programming-matters.html).

## Usage

NOTE: These docs are totally incomplete!  I'll add some more anytime soon.

The main entry point to running concatenative programs is the function
`run-concat`.  It gets a concatenative program, runs it, and returns its
results.

Concatenative programs consist of words (functions represented as keywords) and
data (everything else).

As an example, this is a definition of the factorial function (defined in the
miniconcat language itself) including its application on the number 20.

```
user=> (use 'miniconcat.core)
nil
user=> (run-concat
         'fact 1                                 ;; name & argcount
         [:dup                                   ;; definition
          :zero?                                 ;; condition of :if
          [:ignore 1]                            ;; then-branch
          [:dup :dec :fact :*] :if] :define-word ;; else-branch, and define that as new word

         20 :fact)                               ;; calculate the factorial of 20
2432902008176640000
```

## License

Copyright (C) 2012 Tassilo Horn <tassilo@member.fsf.org>

Distributed under the Eclipse Public License, the same as Clojure.
