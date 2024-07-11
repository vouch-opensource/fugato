# state-gen
Data-first stateful generator for Clojure

## Releases & Dependency Information

## Why state-gen?

There are a couple existing libraries for doing stateful property based testing in
Clojure:

* [stateful-check](https://github.com/czan/stateful-check/tree/master)
* [states](https://github.com/jstepien/states)

These libraries are to varying degrees inspired by features available in
[QuickCheck](http://www.quviq.com/products/erlang-quickcheck/). Often the
motivating examples involve stateful data structures, programming
in the "small", so to speak. The existing literature as well 
libraries often guide usage towards coupling the symbolic part with execution.

But what if you want to test an entire system? 
In this case the complexity of the data structures involved in the method
signatures means a significant amount of generation time is spent generating
things you don't care about when the goal is simply to produce a long of commands.

state-gen emphasizes symbolic generation only, and by guiding the user away
from the specifics of the API permits the generation of many commands without needing
to adopt sophisticated search strategies. 
