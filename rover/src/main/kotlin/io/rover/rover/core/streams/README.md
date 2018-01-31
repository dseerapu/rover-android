# Rover μReactive Streams

A very tiny library within our SDK for handling asynchronous streams in
the style of Reactive Streams, in order to avoid a big, complicated
dependency like RxJava.  It leaves out back-pressure support and complex
operators.

In future, we may be able further simplify the pattern by using Kotlin
continuations/coroutines to allow for imperative style stream code.
