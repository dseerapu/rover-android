package io.rover.rover.core.container

/**
 * Can resolve live instances of objects for a given type.
 */
interface Resolver {
    fun <T: Any> resolve(type: Class<T>): T?

    fun <T: Any> resolveOrFail(type: Class<T>): T {
        return resolve(type) ?: throw RuntimeException("Could not resolve item of type ${type.name}.  Ensure that what you are asking for is the Interface of a Plugin, such as DataPluginInterface.")
    }
}