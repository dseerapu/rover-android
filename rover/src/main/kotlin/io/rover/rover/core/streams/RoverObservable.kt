@file:JvmName("RoverObservable")

package io.rover.rover.core.streams

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.os.Handler
import android.os.Looper
import android.view.View
import io.rover.rover.core.data.http.NetworkTask
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

interface Subscription {
    fun cancel()
}

interface Subscriber<in T> {
    fun onComplete()

    fun onError(error: Throwable)

    fun onNext(item: T)

    fun onSubscribe(subscription: Subscription)
}

interface Publisher<out T> {
    fun subscribe(subscriber: Subscriber<T>)

    companion object {
        internal fun <T> just(item: T): Publisher<T> {
            return object : Publisher<T> {
                override fun subscribe(subscriber: Subscriber<T>) {
                    subscriber.onSubscribe(
                        object : Subscription {
                            override fun cancel() { /* this yields immediately, cancel can have no effect */ }
                        }
                    )
                    subscriber.onNext(item)
                    subscriber.onComplete()
                }
            }
        }

        internal fun <T> empty(): Publisher<T> {
            return object : Publisher<T> {
                override fun subscribe(subscriber: Subscriber<T>) {
                    subscriber.onSubscribe(object : Subscription {
                        override fun cancel() { /* this yields immediately, cancel can have no effect */ }
                    })
                    subscriber.onComplete()
                }
            }
        }

        /**
         * Emit the signals from two or more [Publisher]s without interleaving them.  That is,
         * it will only subscribe to subsequent [sources] (as ordered in the varargs) once prior
         * ones have completed.
         *
         * [concat()](http://reactivex.io/documentation/operators/concat.html).
         */
        internal fun <T> concat(vararg sources: Publisher<T>): Publisher<T> {
            return object : Publisher<T> {
                override fun subscribe(subscriber: Subscriber<T>) {
                    var cancelled = false

                    val subscription = object : Subscription {
                        override fun cancel() {
                            cancelled = true
                        }
                    }

                    subscriber.onSubscribe(subscription)

                    // subscribe to first thing, wait for it to complete, then subscribe to second thing, wait for it to complete, etc.
                    fun recursiveSubscribe(remainingSources: List<Publisher<T>>) {
                        if (remainingSources.isEmpty()) {
                            subscriber.onComplete()
                        } else {
                            // subscribe to the source:
                            remainingSources.first().subscribe(object : Subscriber<T> {
                                override fun onComplete() {
                                    // While there is potentially a risk of a stack overflow here,
                                    // but in practical terms, there will not be that many sources
                                    // given to concat().
                                    if (!cancelled) recursiveSubscribe(
                                        remainingSources.subList(1, remainingSources.size)
                                    )
                                }

                                override fun onError(error: Throwable) {
                                    if (!cancelled) subscriber.onError(error)
                                }

                                override fun onNext(item: T) {
                                    if (!cancelled) subscriber.onNext(item)
                                }

                                override fun onSubscribe(subscription: Subscription) { /* no-op: we don't tell our subscribers about each source subscribing */ }
                            })
                        }
                    }

                    recursiveSubscribe(sources.asList())
                }
            }
        }

        /**
         * Emit the signals from two or more [Publisher]s while interleaving them.  It will
         * subscribe to all of the [sources] when subscribed to.
         *
         * [merge()](http://reactivex.io/documentation/operators/merge.html).
         */
        internal fun <T> merge(vararg sources: Publisher<T>): Publisher<T> {
            return object : Publisher<T> {
                override fun subscribe(subscriber: Subscriber<T>) {
                    var cancelled = false

                    val subscriptions: MutableSet<Subscription> = mutableSetOf()

                    val subscription = object : Subscription {
                        override fun cancel() {
                            cancelled = true
                            // cancel our subscriptions to all the sources
                            subscriptions.forEach { it.cancel() }
                        }
                    }

                    val remainingSources = sources.toMutableSet()

                    subscriber.onSubscribe(subscription)

                    sources.forEach { source ->
                        source.subscribe(object : Subscriber<T> {
                            override fun onComplete() {
                                remainingSources.remove(source)
                                if (remainingSources.isEmpty() && !cancelled) {
                                    subscriber.onComplete()
                                }
                            }

                            override fun onError(error: Throwable) {
                                if (!cancelled) subscriber.onError(error)
                            }

                            override fun onNext(item: T) {
                                if (!cancelled) subscriber.onNext(item)
                            }

                            override fun onSubscribe(subscription: Subscription) {
                                if (!cancelled) {
                                    subscriptions.add(subscription)
                                } else {
                                    // just in case a subscription comes up after we are cancelled
                                    // ourselves, cancel.
                                    subscription.cancel()
                                }
                            }
                        })
                    }
                }
            }
        }

        /**
         * When subscribed to, evaluate the given [builder] block that will yield an Observable
         * that is then subscribed to.
         */
        internal fun <T> defer(builder: () -> Observable<T>): Observable<T> {
            return object : Observable<T> {
                override fun subscribe(subscriber: Subscriber<T>) = builder().subscribe(subscriber)
            }
        }
    }
}

interface Processor<in T, out R> : Subscriber<T>, Publisher<R>

typealias Observable<T> = Publisher<T>

fun <T> Publisher<T>.subscribe(
    onNext: (item: T) -> Unit,
    onError: (throwable: Throwable) -> Unit,
    subscriptionReceiver: ((Subscription) -> Unit)? = null
) {
    this.subscribe(object : Subscriber<T> {
        override fun onComplete() { }

        override fun onError(error: Throwable) { onError(error) }

        override fun onNext(item: T) { onNext(item) }

        override fun onSubscribe(subscription: Subscription) {
            if (subscriptionReceiver != null) {
                subscriptionReceiver(subscription)
            }
        }
    })
}

fun <T> Publisher<T>.subscribe(onNext: (item: T) -> Unit) {
    this.subscribe(object : Subscriber<T> {
        override fun onComplete() { }

        override fun onError(error: Throwable) {
            throw RuntimeException("Undeliverable (unhandled) exception", error)
        }

        override fun onNext(item: T) { onNext(item) }

        override fun onSubscribe(subscription: Subscription) { }
    })
}

fun <T, R> Publisher<T>.map(transform: (T) -> R): Publisher<R> {
    val prior = this
    return object : Publisher<R> {
        override fun subscribe(subscriber: Subscriber<R>) {
            prior.subscribe(
                object : Subscriber<T> {
                    override fun onComplete() {
                        subscriber.onComplete()
                    }

                    override fun onError(error: Throwable) {
                        subscriber.onError(error)
                    }

                    override fun onNext(item: T) {
                        val transformed = try {
                            transform(item)
                        } catch (error: Throwable) {
                            subscriber.onError(Exception("Transform failed in Publisher.map().", error))
                            return
                        }
                        subscriber.onNext(transformed)
                    }

                    override fun onSubscribe(subscription: Subscription) {
                        // for clarity, this is called when I (map()) have subscribed
                        // successfully to the source.  I then want to let the downstream
                        // consumer know that I have subscribed successfully on their behalf,
                        // and also allow them to pass cancellation through.
                        val consumerSubscription = object : Subscription {
                            override fun cancel() { subscription.cancel() }
                        }

                        subscriber.onSubscribe(consumerSubscription)
                    }
                }
            )
        }
    }
}

internal fun <T> Publisher<T>.filter(predicate: (T) -> Boolean): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            this@filter.subscribe(object : Subscriber<T> {
                override fun onComplete() {
                    subscriber.onComplete()
                }

                override fun onError(error: Throwable) {
                    subscriber.onError(error)
                }

                override fun onNext(item: T) {
                    if (predicate(item)) subscriber.onNext(item)
                }

                override fun onSubscribe(subscription: Subscription) {
                    subscriber.onSubscribe(subscription)
                }
            })
        }
    }
}

/**
 * Use this transformation if you have a Publisher of a given type, and you wish to filter it down
 * to only elements of a given subtype.
 *
 * TODO: warn if T is Any, because that probably means consumer is using this transform on a
 * stream with a badly inferred type and thus all of their events could be an unexpected type
 * that will be ignored.
 */
internal inline fun <reified TSub: T, reified T: Any> Publisher<T>.filterForSubtype(): Publisher<TSub> {
    return this.filter { TSub::class.java.isAssignableFrom(it::class.java) } as Publisher<TSub>
}

internal fun <T, R> Publisher<T>.flatMap(transform: (T) -> Publisher<R>): Publisher<R> {
    val prior = this
    return object : Publisher<R> {
        override fun subscribe(subscriber: Subscriber<R>) {
            val outstanding: ConcurrentHashMap<Subscriber<*>, Boolean> = ConcurrentHashMap()

            fun informSubscriberCompleteIfAllCompleted() {
                // TODO: wait for all waiting transform subscriptions to complete too.
                if (outstanding.isEmpty()) {
                    subscriber.onComplete()
                }
            }

            val sourceSubscriber = object : Subscriber<T> {
                override fun onComplete() {
                    outstanding.remove(this)
                    informSubscriberCompleteIfAllCompleted()
                }

                override fun onError(error: Throwable) {
                    subscriber.onError(error)
                }

                override fun onNext(item: T) {
                    val transformPublisher = try {
                        transform(item)
                    } catch ( error: Throwable) {
                        subscriber.onError(Exception("Transform failed in Publisher.flatMap().", error))
                        return
                    }

                    val transformSubscriber = object : Subscriber<R> {
                        override fun onComplete() {
                            outstanding.remove(this)
                            informSubscriberCompleteIfAllCompleted()
                        }

                        override fun onError(error: Throwable) {
                            subscriber.onError(error)
                        }

                        override fun onNext(item: R) {
                            subscriber.onNext(item)
                        }

                        override fun onSubscribe(subscription: Subscription) {
                            outstanding[this] = true
                        }
                    }
                    transformPublisher.subscribe(transformSubscriber)
                }

                override fun onSubscribe(subscription: Subscription) {
                    // for clarity, this is called when I (flatMap()) have subscribed
                    // successfully to the source.  I then want to let the downstream
                    // consumer know that I have subscribed successfully on their behalf,
                    // and also allow them to pass cancellation through.
                    val subscriberSubscription = object : Subscription {
                        override fun cancel() { subscription.cancel() }
                    }

                    subscriber.onSubscribe(subscriberSubscription)
                }
            }

            // we also want to wait for our sourceSubscriber to complete before notifying
            // our subscribers that we have
            outstanding[sourceSubscriber] = true

            prior.subscribe(sourceSubscriber)
        }
    }
}

/**
 * Subscribe to the [Publisher] once, and multicast yielded signals to multiple subscribers.
 *
 * Note that [share] will subscribe to the source once the first consumer subscribes to it.
 *
 * NB. This operator is not yet thread safe.
 */
internal fun <T> Publisher<T>.share(): Publisher<T> {
    val multicastTo: MutableSet<Subscriber<T>> = mutableSetOf()

    var subscribed = false

    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            multicastTo.add(subscriber)

            val subscription = object : Subscription {
                override fun cancel() {
                    // he wants out
                    multicastTo.remove(subscriber)

                    // TODO: once the last subscriber has departed we should unsubscribe the source.
                    // see comments in onSubscribe below.
                }
            }
            subscriber.onSubscribe(subscription)

            // subscribe on initial.
            if (!subscribed) {
                subscribed = true
                this@share.subscribe(
                    object : Subscriber<T> {
                        override fun onComplete() {
                            multicastTo.forEach { it.onComplete() }
                            multicastTo.clear()
                        }

                        override fun onError(error: Throwable) {
                            multicastTo.forEach { it.onError(error) }
                        }

                        override fun onNext(item: T) {
                            multicastTo.forEach { it.onNext(item) }
                        }

                        override fun onSubscribe(subscription: Subscription) {
                            // TODO: once last subscriber has departed we need to unsubscribe
                            // subscription.
                        }
                    }
                )
            }
        }
    }
}

/**
 * Similar to [shareAndReplay], but in addition buffering and re-emitting the [count] most recent
 * events to any new subscriber, it will also immediately subscribe to the source and begin
 * buffering.  This is suitable for use with hot observables.
 *
 * NB. This operator is not yet thread safe.
 */
fun <T> Publisher<T>.shareHotAndReplay(count: Int): Publisher<T> {
    val buffer = ArrayDeque<T>(count)

    val multicastTo: MutableSet<Subscriber<T>> = mutableSetOf()

    this.subscribe(
        object : Subscriber<T> {
            override fun onComplete() {
                multicastTo.forEach { it.onComplete() }
                multicastTo.clear()
            }

            override fun onError(error: Throwable) {
                multicastTo.forEach { it.onError(error) }
            }

            override fun onNext(item: T) {
                multicastTo.forEach { it.onNext(item) }
                buffer.addLast(item)
                // emulate a ring buffer by removing any older entries than `count`
                for (i in 1..buffer.size - count) {
                    buffer.removeFirst()
                }
            }

            override fun onSubscribe(subscription: Subscription) { /* no-op */ }
        }
    )

    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            multicastTo.add(subscriber)

            val subscription = object : Subscription {
                override fun cancel() {
                    // he wants out
                    multicastTo.remove(subscriber)
                }
            }
            subscriber.onSubscribe(subscription)

            // catch up the new subscriber on the `count` number of last events.
            buffer.forEach { event -> subscriber.onNext(event) }
        }
    }
}

/**
 * Similar to [share], but will buffer and re-emit the [count] most recent events to any new
 * subscriber.  Similarly to [share], it will not subscribe to the source until it is first
 * subscribed to itself.
 *
 * @param count the maximum amount of emissions to buffer.  If 0, an infinite number will be
 * buffered.
 *
 * This appears somewhat equivalent to Jake Wharton's
 * [RxReplayingShare](https://github.com/JakeWharton/RxReplayingShare).
 *
 * Not thread safe.
 */
fun <T> Publisher<T>.shareAndReplay(count: Int): Publisher<T> {
    val buffer = ArrayDeque<T>(count)

    val multicastTo: MutableSet<Subscriber<T>> = mutableSetOf()

    var subscribing = false
    var sourceSubscription : Subscription? = null

    fun subscribeSubscriber(subscriber : Subscriber<T>) {
        subscriber.onSubscribe(
            object : Subscription {
                override fun cancel() {
                    // he wants out
                    multicastTo.remove(subscriber)

                    if(multicastTo.isEmpty()) {
                        sourceSubscription?.cancel()
                        subscribing = false
                    }
                }
            }
        )
        buffer.forEach { subscriber.onNext(it) } // bring subscriber up to date with prior items
    }

    fun subscribeToSource() {
        subscribing = true
        this.subscribe(
            object : Subscriber<T> {
                override fun onComplete() {
                    multicastTo.forEach { it.onComplete() }
                    multicastTo.clear()
                }

                override fun onError(error: Throwable) {
                    multicastTo.forEach { it.onError(error) }
                }

                override fun onNext(item: T) {
                    multicastTo.forEach { it.onNext(item) }
                    buffer.addLast(item)
                    // emulate a ring buffer by removing any older entries than `count`
                    if(count != 0) {
                        for (i in 1..buffer.size - count) {
                            buffer.removeFirst()
                        }
                    }
                }

                override fun onSubscribe(subscription: Subscription) {
                    sourceSubscription = subscription
                    subscribing = false
                    multicastTo.forEach { subscriber ->
                        subscribeSubscriber(subscriber)
                    }
                }
            }
        )
    }

    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            multicastTo.add(subscriber)
            // subscribe to source on initial subscribe.
            if (sourceSubscription == null && !subscribing) {
                subscribeToSource()
            } else {
                // we can give them their subscription right away
                subscribeSubscriber(subscriber)
            }
        }
    }
}

internal fun <T: Any> Publisher<T>.first(): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            var sourceSubscription: Subscription? = null

            this@first.subscribe(
                object : Subscriber<T> by subscriber {
                    override fun onComplete() {
                        subscriber.onComplete()
                        sourceSubscription = null
                    }

                    override fun onNext(item: T) {
                        // on first item unsubscribe and complete.
                        subscriber.onNext(item)
                        subscriber.onComplete()
                        sourceSubscription?.cancel()
                        sourceSubscription = null
                    }

                    override fun onSubscribe(subscription: Subscription) {
                        if(sourceSubscription != null) {
                            throw RuntimeException("first() already subscribed to.")
                        }
                        sourceSubscription = subscription
                        subscriber.onSubscribe(
                            object : Subscription {
                                override fun cancel() {
                                    subscription.cancel()
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}

/**
 * Immediately subscribes and buffers events.  Ensures they are delivered once to a subscriber.
 * Any subsequent subscriber will not receive them, and only one subscriber is permitted at a time.
 *
 * Only one subscriber may be active at a time.
 */
internal fun <T: Any> Publisher<T>.exactlyOnce(): Publisher<T> {
    val queue = mutableListOf<T>()

    var currentSubscriber: Subscriber<T>? = null

    this.subscribe(
        object : Subscriber<T> {
            override fun onComplete() {
                currentSubscriber = null
            }

            override fun onError(error: Throwable) {
                val subscriber = currentSubscriber
                if(subscriber != null) {
                    subscriber.onError(error)
                } else {
                    // we won't bother queuing errors.
                }
            }

            override fun onNext(item: T) {
                val subscriber = currentSubscriber
                if(subscriber != null) {
                    subscriber.onNext(item)
                } else {
                    queue.add(item)
                }
            }

            override fun onSubscribe(subscription: Subscription) {
                // we won't bother keeping the subscriber because we will never cancel.
            }
        }
    )

    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            if(currentSubscriber != null) {
                throw RuntimeException("Only one subscriber allowed!")
            }
            currentSubscriber = subscriber

            queue.forEach { subscriber.onNext(it) }
            queue.clear()

            subscriber.onSubscribe(
                object : Subscription {
                    override fun cancel() {
                        currentSubscriber = null
                    }
                }
            )
        }
    }
}

/**
 * Subscribes to the source, stores and re-emit the latest item seen of each of the types to any
 * new subscriber.  Note that this is vulnerable to the typical Java/Android platform issue of
 * type erasure.
 *
 * Note that any re-emitted items are emitted in the order of the [types] given.
 *
 * Not thread safe.
 */
internal fun <T : Any> Publisher<T>.shareAndReplayTypesOnResubscribe(vararg types: Class<out T>): Publisher<T> {
    val lastSeen: MutableMap<Class<out T>, T?> = types.associate { Pair(it, null) }.toMutableMap()

    val shared = this.share()

    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            shared.subscribe(
                object : Subscriber<T> {
                    override fun onComplete() {
                        subscriber.onComplete()
                    }

                    override fun onError(error: Throwable) {
                        subscriber.onError(error)
                    }

                    override fun onNext(item: T) {
                        subscriber.onNext(item)
                        // TODO this has a problem: it does not check for descendant classes, it
                        // must be an exact match.

                        // this will actually be called for every existing subscriber.  thankfully,
                        // setting the lastSeen is an idempotent operation, so it's pretty harmless
                        // to do needless repeats of.
                        if (lastSeen.keys.contains(item.javaClass)) {
                            lastSeen[item.javaClass] = item
                        }
                    }

                    override fun onSubscribe(subscription: Subscription) {
                        subscriber.onSubscribe(subscription)
                        lastSeen.values.filterNotNull().forEach {
                            subscriber.onNext(it)
                        }
                    }
                }
            )
        }
    }
}

internal fun <T> Publisher<T>.doOnSubscribe(behaviour: () -> Unit): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            val wrappedSubscriber = object : Subscriber<T> by subscriber {
                override fun onSubscribe(subscription: Subscription) {
                    behaviour()
                    subscriber.onSubscribe(subscription)
                }
            }
            this@doOnSubscribe.subscribe(wrappedSubscriber)
        }
    }
}

/**
 * Execute the given block when the subscription is cancelled.
 */
internal fun <T> Publisher<T>.doOnUnsubscribe(behaviour: () -> Unit): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {

            // I guess do it on complete & cancel?

            val wrappedSubscriber = object : Subscriber<T> by subscriber {
                override fun onSubscribe(subscription: Subscription) {
                    val wrappedSubscription = object : Subscription {
                        override fun cancel() {
                            behaviour()
                            subscription.cancel()
                        }
                    }
                    subscriber.onSubscribe(wrappedSubscription)
                }

                override fun onComplete() {
                    behaviour()
                    subscriber.onComplete()
                }
            }

            this@doOnUnsubscribe.subscribe(wrappedSubscriber)
        }
    }
}

interface Subject<T> : Processor<T, T>

/**
 *
 */
class PublishSubject<T> : Subject<T> {
    private var subscriber: Subscriber<T>? = null

    override fun subscribe(subscriber: Subscriber<T>) {
        if (this.subscriber != null) {
            throw RuntimeException("PublishSubject() already subscribed.  Consider using .share().")
        }
        this.subscriber = subscriber
        val subscription = object : Subscription {
            override fun cancel() {
                this@PublishSubject.subscriber = null
            }
        }
        subscriber.onSubscribe(subscription)
    }

    override fun onComplete() {
        subscriber?.onComplete()
        subscriber = null
    }

    override fun onError(error: Throwable) {
        subscriber?.onError(error)
    }

    override fun onSubscribe(subscription: Subscription) { /* no-op: any subscribers to the PublishSubject are subscribed immediately */ }

    override fun onNext(item: T) {
        subscriber?.onNext(item)
    }
}

/**
 * Mirrors the source Publisher, but yields an error in the event that the given timeout runs
 * out before the source Publisher emits at least one item.
 *
 * Not thread safe, and uses Android static API (the main looper) and thus assumes subscription
 * and emission on the Android main thread.  Thus will break tests.
 */
internal fun <T> Publisher<T>.timeout(interval: Long, unit: TimeUnit): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            var stillWaiting = true
            this@timeout.subscribe(
                object : Subscriber<T> by subscriber {
                    override fun onComplete() {
                        stillWaiting = false
                        subscriber.onComplete()
                    }

                    override fun onNext(item: T) {
                        stillWaiting = false
                        subscriber.onNext(item)
                    }

                    override fun onError(error: Throwable) {
                        stillWaiting = false
                        subscriber.onError(error)
                    }

                    override fun onSubscribe(subscription: Subscription) {
                        val handler = Handler(Looper.getMainLooper())

                        handler.postDelayed({
                            if(stillWaiting) {
                                // timeout has run out!
                                onError(Throwable("$interval ${unit.name.toLowerCase()} timeout has expired."))
                                subscription.cancel()
                            }
                        }, unit.toMillis(interval))

                        val clientSubscription = object : Subscription {
                            override fun cancel() {

                                // cancel the source:
                                subscription.cancel()

                                // cancel the timer:
                                stillWaiting = false
                            }
                        }

                        subscriber.onSubscribe(clientSubscription)
                    }
                }
            )
        }
    }
}

internal fun <T> Collection<T>.asPublisher(): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            val subscription = object : Subscription {
                override fun cancel() { /* we synchronously emit all (no backpressure), thus cancel is no-op */ }
            }
            subscriber.onSubscribe(subscription)
            this@asPublisher.forEach { item -> subscriber.onNext(item) }
            subscriber.onComplete()
        }
    }
}

/**
 * Execute a side-effect whenever an item is emitted by the Publisher.
 */
internal fun <T> Publisher<T>.doOnNext(callback: (item: T) -> Unit): Publisher<T> {
    val prior = this
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            prior.subscribe(
                object : Subscriber<T> by subscriber {
                    override fun onNext(item: T) {
                        callback(item)
                        subscriber.onNext(item)
                    }

                    override fun onSubscribe(subscription: Subscription) {
                        // for clarity, this is called when I (map()) have subscribed
                        // successfully to the source.  I then want to let the downstream
                        // consumer know that I have subscribed successfully on their behalf,
                        // and also allow them to pass cancellation through.
                        val consumerSubscription = object : Subscription {
                            override fun cancel() { subscription.cancel() }
                        }

                        subscriber.onSubscribe(consumerSubscription)
                    }
                }
            )
        }
    }
}

/**
 * Execute a side-effect whenever an error is emitted by the Publisher.
 */
internal fun <T> Publisher<T>.doOnError(callback: (error: Throwable) -> Unit): Publisher<T> {
    val prior = this
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            prior.subscribe(
                object : Subscriber<T> by subscriber {

                    override fun onError(error: Throwable) {
                        callback(error)
                        subscriber.onError(error)
                    }


                    override fun onSubscribe(subscription: Subscription) {
                        // for clarity, this is called when I (map()) have subscribed
                        // successfully to the source.  I then want to let the downstream
                        // consumer know that I have subscribed successfully on their behalf,
                        // and also allow them to pass cancellation through.
                        val consumerSubscription = object : Subscription {
                            override fun cancel() { subscription.cancel() }
                        }

                        subscriber.onSubscribe(consumerSubscription)
                    }
                }
            )
        }
    }
}

/**
 * Execute a side-effect whenever when the Publisher completes.
 */
internal fun <T> Publisher<T>.doOnComplete(callback: () -> Unit): Publisher<T> {
    val prior = this
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            prior.subscribe(
                object : Subscriber<T> by subscriber {
                    override fun onSubscribe(subscription: Subscription) {
                        // for clarity, this is called when I (map()) have subscribed
                        // successfully to the source.  I then want to let the downstream
                        // consumer know that I have subscribed successfully on their behalf,
                        // and also allow them to pass cancellation through.
                        val consumerSubscription = object : Subscription {
                            override fun cancel() { subscription.cancel() }
                        }

                        subscriber.onSubscribe(consumerSubscription)
                    }

                    override fun onComplete() {
                        callback()
                        subscriber.onComplete()
                    }
                }
            )
        }
    }
}

/**
 * Transform any emitted errors into in-band values.
 */
internal fun <T> Publisher<T>.onErrorReturn(callback: (throwable: Throwable) -> T): Publisher<T> {
    val prior = this
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            prior.subscribe(object : Subscriber<T> by subscriber {
                override fun onError(error: Throwable) {
                    subscriber.onNext(callback(error))
                }
            })
        }
    }
}

// TODO: At such time as we set Android Min SDK to at least 24, change to use Optional here and at
// the usage sites instead (on account of the Reactive Streams spec not actually allowing for
// nulls).
fun <T> Publisher<T?>.filterNulls(): Publisher<T> = filter { it != null }.map { it!! }

/**
 * Republish emissions from the Publisher until such time as the provider [Publisher] [stopper]
 * emits completion, error, or an emission.
 */
internal fun <T, S> Publisher<T>.takeUntil(stopper: Publisher<S>): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            this@takeUntil.subscribe(object : Subscriber<T> {
                override fun onComplete() {
                    subscriber.onComplete()
                }

                override fun onError(error: Throwable) {
                    subscriber.onError(error)
                }

                override fun onNext(item: T) {
                    subscriber.onNext(item)
                }

                override fun onSubscribe(subscription: Subscription) {
                    // subscribe to the stopper and cancel the subscription whenever it emits
                    // anything.
                    stopper.subscribe(object : Subscriber<S> {
                        override fun onComplete() {
                            subscriber.onComplete()
                            subscription.cancel()
                        }

                        override fun onError(error: Throwable) {
                            subscriber.onError(error)
                            subscription.cancel()
                        }

                        override fun onNext(item: S) {
                            subscription.cancel()
                        }

                        override fun onSubscribe(subscription: Subscription) {
                            subscriber.onSubscribe(subscription)
                        }
                    })
                }
            })
        }
    }
}

sealed class ViewEvent {
    class Attach : ViewEvent()
    class Detach : ViewEvent()
}

/**
 * Observe attach and detach events from the given Android [View].
 */
fun View.attachEvents(): Publisher<ViewEvent> {
    return object : Publisher<ViewEvent> {
        override fun subscribe(subscriber: Subscriber<ViewEvent>) {
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(v: View) {
                    subscriber.onNext(ViewEvent.Detach())
                }

                override fun onViewAttachedToWindow(v: View) {
                    subscriber.onNext(ViewEvent.Attach())
                }
            }
            addOnAttachStateChangeListener(listener)

            subscriber.onSubscribe(object : Subscription {
                override fun cancel() {
                    removeOnAttachStateChangeListener(listener)
                }
            })
        }
    }
}

internal fun LifecycleOwner.asPublisher(): Publisher<Lifecycle.Event> {
    return object : Publisher<Lifecycle.Event> {
        override fun subscribe(subscriber: Subscriber<Lifecycle.Event>) {
            val observer = GenericLifecycleObserver { _, event -> subscriber.onNext(event) }
            this@asPublisher.lifecycle.addObserver(observer)
            subscriber.onSubscribe(object : Subscription {
                override fun cancel() {
                    this@asPublisher.lifecycle.removeObserver(observer)
                }
            })
        }
    }
}

/**
 * Returns a [Publisher] that is unsubscribed from [this] when the given [View] is detached.
 */
internal fun <T> Publisher<T>.androidLifecycleDispose(view: View): Publisher<T> {
    return this.takeUntil(
        view.attachEvents().filter { it is ViewEvent.Detach }
    )
}

/**
 * Returns a [Publisher] that is unsubscribed from [this] when the given [LifecycleOwner] (Fragment
 * or Activity) goes out-of-lifecycle.
 */
internal fun <T> Publisher<T>.androidLifecycleDispose(lifecycleOwner: LifecycleOwner): Publisher<T> {
    return this.takeUntil(
        lifecycleOwner.asPublisher().filter { it == Lifecycle.Event.ON_DESTROY }
    )
}

/**
 * When using [asPublisher], you'll find that you will have difficulty specifying a needed type
 * specification for a closure, for that you may use this.  See the [asPublisher] documentation for
 * details.
 */
typealias CallbackReceiver<T> = (T) -> Unit

/**
 * This allows you to map a method call that returns a NetworkTask, a convention in the Rover SDK
 * for async methods, to a [Publisher].
 *
 * Unfortunately, because said convention involves passing in a callback and receiving a
 * [NetworkTask] return value, our adapter here is somewhat convoluted, implemented as an extension
 * method on a closure type.
 *
 * Note that you do need to use [CallbackReceiver]: Kotlin type inference will lack sufficient
 * information to know what your callback type is, and worse, the Kotlin parser does not seem
 * to like nesting closure definitions a closure literal, so the [CallbackReceiver] type-alias
 * becomes necessary.
 *
 * Example usage:
 *
 * `{ callback: CallbackReceiver<MY_RESULT_TYPE> -> graphQlApiService.someMethodThatReturnsANetworkTask(callback) }.asPublisher()`
 */
internal fun <T> (((r: T) -> Unit) -> NetworkTask).asPublisher(): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            var cancelled = false
            val networkTask = this@asPublisher.invoke { result: T ->
                if(cancelled) return@invoke
                subscriber.onNext(result)
                subscriber.onComplete()
            }
            val subscription = object : Subscription {
                override fun cancel() {
                    networkTask.cancel()
                    cancelled = true
                }
            }
            subscriber.onSubscribe(subscription)
            networkTask.resume()
        }
    }
}

/**
 * This will subscribe to Publisher `this` when it is subscribed to itself.  It will execute
 * subscription on the given executor.
 */
internal fun <T> Publisher<T>.subscribeOn(executor: Executor): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            executor.execute {
                // TODO: should we run unsubsriptions on the executor as well?
                this@subscribeOn.subscribe(subscriber)
            }
        }
    }
}

/**
 * This will subscribe to Publisher `this` when it is subscribed to itself.  It will execute
 * subscription on the given executor.
 */
internal fun <T> Publisher<T>.subscribeOn(scheduler: Scheduler): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            scheduler.execute {
                // TODO: should we run unsubsriptions on the executor as well?
                this@subscribeOn.subscribe(subscriber)
            }
        }
    }
}


/**
 * This will subscribe to Publisher `this` when it is subscribed to itself.  It will deliver all
 * callbacks to the subscribing Publisher on the given [executor].
 *
 * Note that the thread you call .subscribe() on remains important: be sure all subscriptions to set
 * up a given Publisher chain are all on a single thread.  Use
 */
internal fun <T> Publisher<T>.observeOn(executor: Executor): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            this@observeOn.subscribe(object: Subscriber<T> {
                override fun onComplete() {
                    executor.execute {
                        subscriber.onComplete()
                    }
                }

                override fun onError(error: Throwable) {
                    executor.execute {
                        subscriber.onError(error)
                    }
                }

                override fun onNext(item: T) {
                    executor.execute {
                        subscriber.onNext(item)
                    }
                }

                override fun onSubscribe(subscription: Subscription) {
                    executor.execute {
                        subscriber.onSubscribe(subscription)
                    }
                }
            })
        }
    }
}

interface Scheduler {
    fun execute(runnable: () -> Unit)

    companion object
}

/**
 * Generate a [Scheduler] for the Android main thread/looper.
 */
internal fun Scheduler.Companion.forAndroidMainThread(): Scheduler {
    val handler = Handler(Looper.getMainLooper())
    return object : Scheduler {
        override fun execute(runnable: () -> Unit) {
            handler.post(runnable)
        }
    }
}

fun <T> Publisher<T>.observeOn(scheduler: Scheduler): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            this@observeOn.subscribe(object: Subscriber<T> {
                override fun onComplete() {
                    scheduler.execute {
                        subscriber.onComplete()
                    }
                }

                override fun onError(error: Throwable) {
                    scheduler.execute {
                        subscriber.onError(error)
                    }
                }

                override fun onNext(item: T) {
                    scheduler.execute {
                        subscriber.onNext(item)
                    }
                }

                override fun onSubscribe(subscription: Subscription) {
                    scheduler.execute {
                        subscriber.onSubscribe(subscription)
                    }
                }
            })
        }
    }
}

/**
 * Block the thread waiting for the publisher to complete.
 *
 * All emitted items are buffered into a list that is then returned.
 */
internal fun <T> Publisher<T>.blockForResult(afterSubscribe: () -> Unit = {}): List<T> {
    val latch = CountDownLatch(1)
    var receivedError: Throwable? = null
    val results : MutableList<T> = mutableListOf()

    this.subscribe(object : Subscriber<T> {
        override fun onComplete() {
            latch.countDown()
        }

        override fun onError(error: Throwable) {
            receivedError = error
            latch.countDown()
        }

        override fun onNext(item: T) {
            results.add(item)
        }

        override fun onSubscribe(subscription: Subscription) {
            afterSubscribe()
        }
    })

    if(!latch.await(10, TimeUnit.SECONDS)) {
        throw Exception("Reached timeout while blocking for publisher! Items received: ${results.count()}")
    }

    if(receivedError != null) {
        throw Exception("Error while blocking on Publisher.  Items received: ${results.count()}", receivedError)
    }

    return results
}