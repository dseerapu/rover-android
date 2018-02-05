package io.rover.rover.core.container

import io.rover.rover.core.logging.log

/**
 * Implements both [Container] and [Resolver].  Thus, it hosts a set of live objects for a given set
 * of types, and delegates instantiation of them to a set of [Assembler]s.
 *
 * Can be thought of as mildly analogous to a Dagger or Koin module.
 */
class PluginContainer(
    assemblers: List<Assembler>
) : Container, Resolver, ContainerResolver {
    private var registeredPlugins: HashMap<ServiceKey<*>, ServiceEntryInterface> = hashMapOf()

    init {
        assemblers.forEach { it.register(this) }
        assemblers.forEach { it.assemble(this) }
    }

    override fun <T: Any> resolve(type: Class<T>): T? {
        val exampleFactory = { _: Resolver -> null }

        val factoryType = exampleFactory.javaClass

        log.v("Attempting to resolve Plugin of type: $factoryType")

        val key = ServiceKey(type)
        // retrieve the item of type from the registered plugins hash.  However, because I have a
        // generic type for the entry, good ol' Java type erasure rears its head.  However, I know
        // that the entry is consistent with the key, so the unchecked cast is safe.
        @Suppress("UNCHECKED_CAST")
        val entry = (registeredPlugins[key] ?: return null) as ServiceEntry<T>

        val factory = entry.factory

        return entry.instance ?: factory(this).apply {
            // if constructing a new instance, replace the Entry in the list with one that has
            // the memoized/cached instance.
            log.v("Registering instance of ${this.javaClass.name} for type $key as singleton")
            registeredPlugins[key] = entry.copy(instance = this)
        }
    }


    override fun <T: Any> register(type: Class<T>, factory: (Resolver) -> T) {
        val key = ServiceKey(type)
        val entry = ServiceEntry(type, factory)
        registeredPlugins[key] = entry
    }

    data class ServiceKey<T>(
        val factoryType: Class<T>
    )

    interface ServiceEntryInterface

    data class ServiceEntry<T>(
        val serviceType: Class<T>,
        val factory: (Resolver) -> T,
        /**
         * The ServiceEntry will memoize (cache) the object instantiated by the factory. That is, it
         * will behave as a singleton.
         */
        val instance: T? = null
    ): ServiceEntryInterface
}

