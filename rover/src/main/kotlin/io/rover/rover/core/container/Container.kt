package io.rover.rover.core.container

/**
 * Contains a mapping of factories to object types.
 */
interface Container {
    /**
     * Register a [factory] with this Container that will instantiate objects for the given [type].
     * The Factory is a simple closure that, given only a [Resolver] (an interface by which it can
     * acquire its dependencies), will instantiate the item of the supplied type.
     */
    fun <T> register(type: Class<T>, factory: (Resolver) -> T)
}
