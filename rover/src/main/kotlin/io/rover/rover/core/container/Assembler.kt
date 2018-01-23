package io.rover.rover.core.container

interface Assembler {
    fun register(container: Container) { }

    fun assemble(resolver: Resolver) { }
}