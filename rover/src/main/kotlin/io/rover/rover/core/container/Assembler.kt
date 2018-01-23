package io.rover.rover.core.container

/**
 * Assemblers define factories (themselves typically just closures) that can then provide
 * objects needed for given types in [Container]s.
 */
interface Assembler {
    /**
     * Register any factories this Assembler can provide in the given [Container].
     */
    fun register(container: Container) { }

    /**
     * Allow for any post-wireup side effects.
     */
    fun assemble(resolver: Resolver) { }
}
