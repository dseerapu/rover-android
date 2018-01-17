@file:JvmName("RoverSingle")

package io.rover.rover.streams

import java.util.*

interface SingleSubscription<T> {
    // TODO: technically needs unsubscription support.  but it's not a priority.
}

interface Scheduler {
    /**
     * Schedule some sort of synchronous operation (computation or blocking I/O) to happen
     * on this scheduler.  Returns a hot [Single] (that is, it will not wait until it is subscribed
     * to in order to begin the operation).
     */
    fun <T> scheduleOperation(operation: () -> T): Single<T>

    /**
     * Run a closure that delivers its result as a side-effect.  Meant for use by subscribers.
     */
    fun scheduleSideEffectOperation(operation: () -> Unit)
}

interface SingleSubscriber<T>: SingleSubscription<T> {
    fun onCompleted(value: T)

    fun onError(error: Throwable)
}

/**
 * An observable event source that can only emit one event (or, in lieu, an error).
 *
 * Can be subscribed to by a [SingleSubscriber] (or just with two closures for convenience).
 */
interface Single<T> {
    /**
     * Start observing this single.  The returned subscriber will be executed on the given
     * scheduler.
     */
    fun subscribe(subscriber: SingleSubscriber<T>, scheduler: Scheduler): SingleSubscription<T>

    companion object {
        fun <T> just(value: T): Single<T> {
            return object : SinglePublisher<T>() {
                override fun subscribe(subscriber: SingleSubscriber<T>, scheduler: Scheduler): SingleSubscription<T> {
                    val subscription = super.subscribe(subscriber, scheduler)
                    subscriber.onCompleted(value)
                    return subscription
                }
            }
        }
    }
}

/**
 * Subscribe to this single.
 */
fun <T> Single<T>.subscribe(
    completed: (T) -> Unit,
    error: (Throwable) -> Unit,
    scheduler: Scheduler
): SingleSubscription<T> {
    return subscribe(object : SingleSubscriber<T> {
        override fun onCompleted(value: T) {
            completed(value)
        }

        override fun onError(error: Throwable) {
            error(error)
        }
    }, scheduler)
}

/**
 * Listen to this asynchronous event source in the context of an Android activity with a
 * simple callback that will be called on the main thread when the result is ready.
 *
 * This is a convenience method for consumers who are not familiar with reactive streams.
 */
fun <T> Single<T>.call(
    callback: (T) -> Unit
) {
    // TODO: mainthreadscheduler should be injected.  Thankfully it's a light object to
    // construct.  These methods should be moved out of the
    // interface.
    // TODO: should probably have a story for lifecycle composition

    val mainThreadScheduler = MainThreadScheduler()
    subscribe(callback, { error -> throw error }, mainThreadScheduler)
}

/**
 * Map the item emitted by this Single by way of a predicate.
 */
fun <T, M> Single<T>.map(processingScheduler: Scheduler, predicate: (T) -> M): Single<M> {
    val prior: Single<T> = this
    return object : SinglePublisher<M>() {
        override fun subscribe(subscriber: SingleSubscriber<M>, scheduler: Scheduler): SingleSubscription<M> {
            val subscription = super.subscribe(subscriber, scheduler)

            val subject = this
            // now subscribe to the prior on behalf of our subscriber
            prior.subscribe(
                { value ->
                    try {
                        // and emit the prior's emission, after transforming it.
                        subject.onCompleted(
                            // apply the transform
                            predicate(value)
                        )
                    } catch (e: Throwable) {
                        subject.onError(e)
                    }
                }, { error ->
                subject.onError(error)
            },
                scheduler
            )
            return subscription
        }
    }
}

/**
 * An implementation of [SingleSubscriber] that implements maintaining a list of Subscribers.
 */
open class SinglePublisher<T>: Single<T>, SingleSubscriber<T> {
    private val subscriptions = Collections.synchronizedSet(mutableSetOf<Pair<SingleSubscriber<T>, Scheduler>>())

    override fun onCompleted(value: T) {
        subscriptions.forEach { it.second.scheduleSideEffectOperation { it.first.onCompleted(value) }  }
    }

    override fun onError(error: Throwable) {
        subscriptions.forEach { it.second.scheduleSideEffectOperation { it.first.onError(error) } }
    }

    override fun subscribe(subscriber: SingleSubscriber<T>, scheduler: Scheduler): SingleSubscription<T> {
        subscriptions.add(
            Pair(subscriber, scheduler)
        )

        // the subscriber itself is the subscription, for now.
        return subscriber
    }
}
