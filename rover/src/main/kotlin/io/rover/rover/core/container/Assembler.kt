package io.rover.rover.core.container

/**
 * Assemblers define factories (themselves typically just closures) that can then provide
 * objects needed for given types in [Container]s.
 *
 * They are usually used to group together the registrations of multiple factories in a
 * given vertical concern.
 */
interface Assembler {
    /**
     * Register any factories this Assembler can provide in the given [Container].
     */
    fun assemble(container: Container) { }

    /**
     * Allow for any post-wireup side effects.
     */
    fun afterAssembly(resolver: Resolver) { }
}
