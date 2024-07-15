# fugato

Data oriented stateful test-check generator for Clojure & ClojureScript.

## Releases & Dependency Information

## Why fugato?

There are already a couple existing libraries for doing stateful property based
testing in Clojure:

* [stateful-check](https://github.com/czan/stateful-check/tree/master)
* [states](https://github.com/jstepien/states)

These libraries are to varying degrees inspired by features available in
[QuickCheck](http://www.quviq.com/products/erlang-quickcheck/). Often the
motivating examples involve stateful data structures and programming in the
"small", so to speak. In addition, the existing literature as well libraries
couple the underlying symbolic generation with direct execution against a 
concrete API.

What if you want to test an entire system? In this case, the complexity of the
data structures involved in specific API method signatures means a significant
amount of generation time is consumed on details you don't care about when the
goal is simply to produce a long sequence of commands.

Thus, fugato emphasizes symbolic generation only, and by guiding the user away from
the specifics of the API permits the efficient generation of long command
sequences without needing to adopt sophisticated search strategies.

Once the command sequence is generated these be can run however you see fit.
In this sense fugato is not a testing framework or even a methodology. It simply
generates commands and supports shrinking. The user can leverage existing
testing tools without having to learn new concepts beyond the task of command 
generation.

Since fugato is abstract, the focus is primarily on modeling your domain. In
particular since there is no pressure to program against any real interface, it's
natural to model complex scenarios involving multiple actors and
multiple manipulatable objects with complex relationships. Once the modeling
is done you can choose how to run the commands against your real system.

## Defining a Model

A model is a map of command specifications:

```clojure
{:withdraw withdraw-command-spec
 :deposit  deposit-command-spec}
```

## Defining a Command Spec

A command spec is a map that looks like the following:

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

`:valid?` - A function that takes state and a command a determines whether a command
should discard during shrinking (the process by which a minimal failing command
sequence is discovered) During shrinking commands are dropped one by one. This 
function is used determine whether later commands are no longer valid if a prior 
command has been removed.

## Example

The hardest part about learning how to use fugato is learning how to write
test-check generators, so we've provided a 
[motivating example and tutorial](https://github.com/vouch-opensource/fugato/wiki/Guide-%26-Tutorial).

## Contributing

Currently fugato is only taking bug reports. If you find a bug or would like
to see an enhancement that aligns with the following design principles
please file a Github issue!

#### Design Principles

* Symbolic generation only. Bring your own runner!

## License ##

    Copyright (c) Vouch, Inc. All rights reserved. The use and
    distribution terms for this software are covered by the Eclipse
    Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
    which can be found in the file epl-v10.html at the root of this
    distribution. By using this software in any fashion, you are
    agreeing to be bound by the terms of this license. You must
    not remove this notice, or any other, from this software.
