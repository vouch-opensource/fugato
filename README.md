# state-gen
Data-first stateful generator for Clojure

## Releases & Dependency Information

## Why state-gen?

There are already couple existing libraries for doing stateful property based
testing in Clojure:

* [stateful-check](https://github.com/czan/stateful-check/tree/master)
* [states](https://github.com/jstepien/states)

These libraries are to varying degrees inspired by features available in
[QuickCheck](http://www.quviq.com/products/erlang-quickcheck/). Often the
motivating examples involve stateful data structures, programming in the
"small", so to speak. In addition, the existing literature as well libraries
often guide usage towards coupling the symbolic part with execution.

What if you want to test an entire system? In this case the complexity of
the data structures involved in specific method signatures means a significant amount
of generation time is consumed on aspects you don't care about when the goal
is simply to produce a long of commands.

state-gen emphasizes symbolic generation only, and by guiding the user away from
the specifics of the API permits the efficient generation of long command
sequences without needing to adopt sophisticated search strategies.

Once the command sequence is generated these can run however you see fit. These
can also be written to files which can be run later as sophisticated regression 
tests unlikely to every be written or maintained by a human. 
