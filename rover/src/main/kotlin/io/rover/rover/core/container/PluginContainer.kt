package io.rover.rover.core.container

/**
 * Implements both [Container] and [Resolver].  Thusly, it hosts a set of live objects for a given
 * set of types, and delegates instantiation of them to a set of [Assembler]s.
 *
 * Can be thought of as mildly analogous to a Dagger or Koin module.
 */
class PluginContainer(
    assemblers: List<Assembler>
) : Container, Resolver, ContainerResolver {
    private var registeredPlugins: HashMap<ServiceKey, ServiceEntryInterface> = hashMapOf()

    init {
        assemblers.forEach { it.register(this) }
        assemblers.forEach { it.assemble(this) }
    }

    override fun <T> resolve(type: Class<T>): T? {
        val key = ServiceKey(type as Class<ServiceFactory>) //TODO: change resolve() to accept type: Class<ServiceFactory>

        val entry = (registeredPlugins[key] ?: return null) as ServiceEntry<T>
        val factory = entry.factory as (Resolver) -> T

        return entry.instance ?: factory(this).apply {
            // if constructing a new instance, replace the Entry in the list with one that has
            // the memoized/cached instance.
            registeredPlugins[key] = entry.copy(instance = this)
        }
    }

    override fun <T> register(type: Class<T>, factory: (Resolver) -> T) {
        val key = ServiceKey(factory.javaClass)
        val entry = ServiceEntry(type, factory)
        registeredPlugins[key] = entry
    }

    data class ServiceKey(
        val factoryType: Class<ServiceFactory>
    )

    interface ServiceEntryInterface

    data class ServiceEntry<T>(
        val serviceType: Class<T>,
        val factory: ServiceFactory,
        /**
         * The ServiceEntry will memoize (cache) the object instantiated by the factory. That is, it
         * will behave as a singleton.
         */
        val instance: T? = null
    ): ServiceEntryInterface
}

