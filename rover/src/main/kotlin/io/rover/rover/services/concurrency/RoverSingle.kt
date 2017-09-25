package io.rover.rover.services.concurrency

import io.rover.rover.core.logging.log
import java.util.*

// I think I can come up with a very basic "Rx-lite" here.

// Single.observeOn(subscriber, someSortOfRoverScheduler).


interface Subscription<T> {
    // TODO: technically needs unsubscription support.  but it's not a priority.
}

interface Scheduler {
    /**
     * Schedule some sort of synchronous operation (computation or blocking I/O) to happen
     * on this scheduler.  Returns a hot Single (that is, it will not wait until it is subscribed
     * to in order to begin the operation).
     */
    fun <T> scheduleOperation(operation: () -> T): Single<T>

    /**
     * Run a closure that delivers its result as a side-effect.  Meant for use by subscribers.
     */
    fun scheduleSideEffectOperation(operation: () -> Unit)
}

interface Subscriber<T>: Subscription<T> {
    fun completed(value: T)

    fun error(error: Throwable)
}

interface Single<T> {
    /**
     * Start observing this single.  The returned subscriber will be executed on the given
     * scheduler.
     */
    fun subscribe(subscriber: Subscriber<T>, scheduler: Scheduler): Subscription<T>

    /**
     * Start observing this single.
     *
     * TODO: this façade method should be factored out; having it overridable here has already caused 1 nasty bug.
     *
     * THIS MUST NOT BE OVERRIDDEN TO ADD BEHAVIOUR
     */

    fun subscribe(
        completed: (T) -> Unit,
        error: (Throwable) -> Unit,
        scheduler: Scheduler
    ): Subscription<T> {
        return subscribe(object : Subscriber<T> {
            override fun completed(value: T) {
                completed(value)
            }

            override fun error(error: Throwable) {
                error(error)
            }
        }, scheduler)
    }

    companion object {
        fun <T> just(value: T): Single<T> {
            return object : Subject<T>() {
                override fun subscribe(subscriber: Subscriber<T>, scheduler: Scheduler): Subscription<T> {
                    val subscription = super.subscribe(subscriber, scheduler)
                    subscriber.completed(value)
                    return subscription
                }
            }
        }
    }

    /**
     * Map the item emitted by this Single by way of a predicate.
     */
    fun <M> map(processingScheduler: Scheduler, predicate: (T) -> M): Single<M> {
        val prior: Single<T> = this
        return object : Subject<M>() {
            override fun subscribe(subscriber: Subscriber<M>, scheduler: Scheduler): Subscription<M> {
                val subscription = super.subscribe(subscriber, scheduler)

                val subject = this
                // now subscribe to the prior on behalf of our subscriber
                prior.subscribe(
                    { value ->
                        try {
                            // and emit the prior's emission, after transforming it.
                            subject.completed(
                                // apply the transform
                                predicate(value)
                            )
                        } catch (e: Throwable) {
                            subject.error(e)
                        }
                    }, { error ->
                        subject.error(error)
                    },
                    scheduler
                )
                return subscription
            }
        }
    }
}


open class Subject<T>: Single<T>, Subscriber<T> {
    private val subscriptions = Collections.synchronizedSet(mutableSetOf<Pair<Subscriber<T>, Scheduler>>())

    override fun completed(value: T) {
        subscriptions.forEach { it.second.scheduleSideEffectOperation { it.first.completed(value) }  }
    }

    override fun error(error: Throwable) {
        subscriptions.forEach { it.second.scheduleSideEffectOperation { it.first.error(error) } }
    }

    override fun subscribe(subscriber: Subscriber<T>, scheduler: Scheduler): Subscription<T> {
        subscriptions.add(
            Pair(subscriber, scheduler)
        )

        // the subscriber itself is the subscription, for now.
        return subscriber
    }
}
