package io.rover.rover.core.container

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
        assemblers.forEach { it.assemble(this) }
        assemblers.forEach { it.afterAssembly(this) }
    }

    // TODO: consider trapping any late-binding exceptions caused by the mess of unchecked casts and
    // emitting somewhat more helpful errors.

    override fun <T: Any> resolve(type: Class<T>, name: String?): T? {
        return doResolve<T, () -> T>(type, name)?.invoke()
    }

    override fun <T : Any, Arg1 : Any> resolve(type: Class<T>, name: String?, arg1: Arg1): T? {
        return doResolve<T, (Arg1) -> T>(type, name)?.invoke(arg1)
    }

    override fun <T : Any, Arg1 : Any, Arg2: Any> resolve(type: Class<T>, name: String?, arg1: Arg1, arg2: Arg2): T? {
        return doResolve<T, (Arg1, Arg2) -> T>(type, name)?.invoke(arg1, arg2)
    }

    override fun <T : Any, Arg1 : Any, Arg2: Any, Arg3: Any> resolve(type: Class<T>, name: String?, arg1: Arg1, arg2: Arg2, arg3: Arg3): T? {
        return doResolve<T, (Arg1, Arg2, Arg3) -> T>(type, name)?.invoke(arg1, arg2, arg3)
    }

    private fun <TClass: Any, TFactory: Any> doResolve(type: Class<*>, name: String?): TFactory? {
        val key = ServiceKey(type, name)

        // retrieve the item of type from the registered plugins hash.  However, because I have a
        // generic type for the entry, good ol' Java type erasure rears its head.  However, I know
        // that the entry is consistent with the key, so the unchecked cast is safe.
        @Suppress("UNCHECKED_CAST")
        val entry = (registeredPlugins[key] ?: return null) as ServiceEntry<TClass>

        when (entry) {
            is ServiceEntry.Transient<TClass> -> {
                @Suppress("UNCHECKED_CAST")
                return entry.factory as TFactory
            }
            is ServiceEntry.Singleton<TClass> -> {
                @Suppress("UNCHECKED_CAST")
                return {
                    entry.instance ?: entry.factory(this).apply {
                        // if constructing a new instance, replace the Entry in the list with one that has
                        // the memoized/cached instance.
                        // log.v("Registering instance of ${this.javaClass.name} for type $key as singleton")
                        registeredPlugins[key] = entry.copy(instance = this)
                    }
                } as TFactory
            }
        }
    }

    private fun <T: Any> doRegister(scope: Scope, type: Class<T>, factory: (Resolver) -> T, name: String? = null) {
        val entry = when(scope) {
            Scope.Singleton -> ServiceEntry.Singleton<T>(factory)
            Scope.Transient -> ServiceEntry.Transient<T>(factory)
        }
        val key = ServiceKey(type, name)
        registeredPlugins[key] = entry
    }

    private fun <T: Any> doRegisterWithArgs(scope: Scope, type: Class<T>, factory: Any, name: String? = null) {
        if(scope != Scope.Transient) {
            throw RuntimeException("You may only use factory arguments with Scope.Transient.")
        }
        val entry = ServiceEntry.Transient<T>(factory)
        val key = ServiceKey(type, name)
        registeredPlugins[key] = entry
    }


    override fun <T : Any> register(scope: Scope, type: Class<T>, name: String?, factory: (Resolver) -> T) {
        doRegister(scope, type, factory, name)
    }

    override fun <T : Any, Arg1 : Any> register(scope: Scope, type: Class<T>, name: String?, factory: (Resolver, Arg1) -> T) {
        doRegisterWithArgs(scope, type, factory, name)
    }

    override fun <T : Any, Arg1 : Any, Arg2: Any> register(scope: Scope, type: Class<T>, name: String?, factory: (Resolver, Arg1, Arg2) -> T) {
        doRegisterWithArgs(scope, type, factory, name)
    }

    override fun <T : Any, Arg1 : Any, Arg2: Any, Arg3: Any> register(scope: Scope, type: Class<T>, name: String?, factory: (Resolver, Arg1, Arg2, Arg3) -> T) {
        doRegisterWithArgs(scope, type, factory, name)
    }

    data class ServiceKey<T>(
        val factoryType: Class<T>,
        val name: String? = null
    )

    interface ServiceEntryInterface

    sealed class ServiceEntry<T: Any>: ServiceEntryInterface {
        /**
         * This entry will dynamically create its instance on first use, and then continue
         * to return it throughout the lifetime of the Container.
         *
         * It does not support parameters.
         */
        data class Singleton<T: Any>(
            val factory: (Resolver) -> T,

            /**
             * The ServiceEntry will memoize (cache) the object instantiated by the factory. That is, it
             * will behave as a singleton.
             */
            val instance: T? = null
        ): ServiceEntry<T>()

        /**
         * This entry will yield a new instance of its type on every evaluation.
         *
         * It can support parameters.
         */
        data class Transient<T: Any>(
            /**
             * This is a closure that will manufacture an instance of [T].  Cannot be fully typed
             *
             * Type is:
             *
             * `(Resolver, **Args) -> T`
             */
            val factory: Any
        ): ServiceEntry<T>()
    }
}
