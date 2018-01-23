package io.rover.rover.core.container

/**
 * Can resolve live instances of objects from a given type.
 */
interface Resolver {
    fun <T> resolve(type: Class<T>): T?
}