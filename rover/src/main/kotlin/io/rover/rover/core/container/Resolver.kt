package io.rover.rover.core.container

/**
 * Can resolve live instances of objects from a given type.
 */
interface Resolver {
    fun <T: Any> resolve(type: Class<T>): T?
}