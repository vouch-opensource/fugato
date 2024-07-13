# state-gen
Data-first stateful generator for Clojure

## Releases & Dependency Information

## Why state-gen?

There are already a couple existing libraries for doing stateful property based
testing in Clojure:

* [stateful-check](https://github.com/czan/stateful-check/tree/master)
* [states](https://github.com/jstepien/states)

These libraries are to varying degrees inspired by features available in
[QuickCheck](http://www.quviq.com/products/erlang-quickcheck/). Often the
motivating examples involve stateful data structures and programming in the
"small", so to speak. In addition, the existing literature as well libraries
often guide usage towards coupling the symbolic part with execution.

What if you want to test an entire system? In this case the complexity of the
data structures involved in specific method signatures means a significant
amount of generation time is consumed on aspects you don't care about when the
goal is simply to produce a long sequence of commands.

Thus, state-gen emphasizes symbolic generation only, and by guiding the user away from
the specifics of the API permits the efficient generation of long command
sequences without needing to adopt sophisticated search strategies.

Once the command sequence is generated these be can run however you see fit.
In this sense state-gen is not a testing framework or even a methodology. It simply
generates commands and supports shrinking. The user can leverage existing
testing tools without having to learn new concepts beyond the task of command 
generation.

## Defining a Model

A model is a map of command specifications:

```clojure
{:withdraw withdraw-command-spec
 :deposit  deposit-command-spec}
```

## Defining a Command Spec

A command spec is map that looks like the following:

```clojure
{:args       args-generator
 :freq       an-integer
 :next-state next-state-fn
 :run?       run?-fn
 :valid?     valid?-fn}
```

`args-generator` - a function that takes a state and returns a generator for
the arguments of the command. Only this part requires a basic understanding how
`clojure.test.check.generators` works.

`:freq` - an integer to set the likelihood of the command to be generated.
Defaults to `1`.

`:next-state` - a function take a state and the generated command and computes
the next state.

`:run?` - a function that takes a state to determine whether a particular command can be
generated. Instead of randomly generating commands, you can inspect the state
and control which commands are available at each step.

`:valid?` - during shrinking commands are dropped one by one. This function is used
determine whether later commands are no longer valid if a prior command has been
removed.
