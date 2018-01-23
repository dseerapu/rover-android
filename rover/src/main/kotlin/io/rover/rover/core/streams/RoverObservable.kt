@file:JvmName("RoverObservable")

package io.rover.rover.core.streams

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.os.Looper
import android.view.View
import io.rover.rover.plugins.data.NetworkTask
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

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
        fun <T> just(item: T): Publisher<T> {
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

        /**
         * Emit the signals from two or more [Publisher]s without interleaving them.  That is,
         * it will only subscribe to subsequent [sources] (as ordered in the varargs) once prior
         * ones have completed.
         *
         * [concat()](http://reactivex.io/documentation/operators/concat.html).
         */
        fun <T> concat(vararg sources: Publisher<T>): Publisher<T> {
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
        fun <T> merge(vararg sources: Publisher<T>): Publisher<T> {
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
        fun <T> defer(builder: () -> Observable<T>): Observable<T> {
            return object : Observable<T> {
                override fun subscribe(subscriber: Subscriber<T>) = builder().subscribe(subscriber)
            }
        }
    }
}

interface Processor<T, R> : Subscriber<T>, Publisher<R>

typealias Observable<T> = Publisher<T>

fun <T> Publisher<T>.subscribe(onNext: (item: T) -> Unit, onError: (throwable: Throwable) -> Unit, subscriptionReceiver: ((Subscription) -> Unit)? = null) {
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
                        if (Looper.myLooper() != Looper.getMainLooper()) {
                            throw RuntimeException("Completion result on bogus thread?!  Running on thread ${Thread.currentThread()}")
                        }

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

                    override fun onSubscribe(sourceSubscription: Subscription) {
                        // for clarity, this is called when I (map()) have subscribed
                        // successfully to the source.  I then want to let the downstream
                        // consumer know that I have subscribed successfully on their behalf,
                        // and also allow them to pass cancellation through.
                        val subscription = object : Subscription {
                            override fun cancel() { sourceSubscription.cancel() }
                        }

                        subscriber.onSubscribe(subscription)
                    }
                }
            )
        }
    }
}

fun <T> Publisher<T>.filter(predicate: (T) -> Boolean): Publisher<T> {
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

fun <T, R> Publisher<T>.flatMap(transform: (T) -> Publisher<R>): Publisher<R> {
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

                override fun onSubscribe(sourceSubscription: Subscription) {
                    // for clarity, this is called when I (flatMap()) have subscribed
                    // successfully to the source.  I then want to let the downstream
                    // consumer know that I have subscribed successfully on their behalf,
                    // and also allow them to pass cancellation through.
                    val subscription = object : Subscription {
                        override fun cancel() { sourceSubscription.cancel() }
                    }

                    subscriber.onSubscribe(subscription)
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
 */
fun <T> Publisher<T>.share(): Publisher<T> {
    val multicastTo: MutableSet<Subscriber<T>> = mutableSetOf()

    var subscribed = false

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

                        override fun onSubscribe(subscription: Subscription) { /* no-op */ }
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
 * This appears somewhat equivalent to Jake Wharton's
 * [RxReplayingShare](https://github.com/JakeWharton/RxReplayingShare).
 *
 * Not thread safe.
 */
fun <T> Publisher<T>.shareAndReplay(count: Int): Publisher<T> {
    val buffer = ArrayDeque<T>(count)

    val multicastTo: MutableSet<Subscriber<T>> = mutableSetOf()

    var subscribed = false

    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            // subscribe to source on initial subscribe.
            if (!subscribed) {
                // note that this is not re-entrant without a race condition.
                subscribed = true
                this@shareAndReplay.subscribe(
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

                        override fun onSubscribe(subscription: Subscription) {
                            // catch up the new subscriber on the `count` number of last events.
                            multicastTo.forEach { subscriber ->
                                buffer.forEach { event -> subscriber.onNext(event) }
                            }
                        }
                    }
                )
            }

            multicastTo.add(subscriber)

            val subscription = object : Subscription {
                override fun cancel() {
                    // he wants out
                    multicastTo.remove(subscriber)
                }
            }
            subscriber.onSubscribe(subscription)
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
fun <T : Any> Publisher<T>.shareAndReplayTypesOnResubscribe(vararg types: Class<out T>): Publisher<T> {
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

/**
 * Execute the given block when the Publisher is either cancelled or co
 */
fun <T> Publisher<T>.doOnUnsubscribe(behaviour: () -> Unit): Publisher<T> {
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
    var subscriber: Subscriber<T>? = null

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

fun <T> Collection<T>.asPublisher(): Publisher<T> {
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

@Deprecated("Whenever we set Android Min SDK to at least 24, change to use Optional here instead (on account of the Reactive Streams spec not actually allowing for nulls)")
fun <T> Publisher<T?>.filterNulls(): Publisher<T> = filter { it != null }.map { it!! }

/**
 * Republish emissions from the Publisher until such time as the provider [Publisher] [stopper]
 * emits completion, error, or an emission.
 */
fun <T, S> Publisher<T>.takeUntil(stopper: Publisher<S>): Publisher<T> {
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

fun LifecycleOwner.asPublisher(): Publisher<Lifecycle.Event> {
    return object : Publisher<Lifecycle.Event> {
        override fun subscribe(subscriber: Subscriber<Lifecycle.Event>) {
            val observer = object : GenericLifecycleObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    subscriber.onNext(event)
                }
            }
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
fun <T> Publisher<T>.androidLifecycleDispose(view: View): Publisher<T> {
    return this.takeUntil(
        view.attachEvents().filter { it is ViewEvent.Detach }
    )
}

/**
 * Returns a [Publisher] that is unsubscribed from [this] when the given [LifecycleOwner] (Fragment
 * or Activity) goes out-of-lifecycle.
 */
fun <T> Publisher<T>.androidLifecycleDispose(lifecycleOwner: LifecycleOwner): Publisher<T> {
    return this.takeUntil(
        lifecycleOwner.asPublisher().filter { it == Lifecycle.Event.ON_STOP || it == Lifecycle.Event.ON_DESTROY }
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
 * to like nesting closure definitions a closure literal, so the [CallbackReceiver] typelias
 * becomes necessary.
 *
 * Example usage:
 *
 * `{ callback: CallbackReceiver<MY_RESULT_TYPE> -> roverNetworkService.someMethodThatReturnsANetworkTask(callback) }.asPublisher()`
 */
fun <T> (((r: T) -> Unit) -> NetworkTask).asPublisher(): Publisher<T> {
    return object : Publisher<T> {
        override fun subscribe(subscriber: Subscriber<T>) {
            val networkTask = this@asPublisher.invoke { result: T ->
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    throw RuntimeException("DataPlugin did not dispatch result handler to main thread correctly.  Running on thread ${Thread.currentThread()}")
                }
                subscriber.onNext(result)
                subscriber.onComplete()
            }
            val subscription = object : Subscription {
                override fun cancel() {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        throw RuntimeException("DataPlugin did not dispatch cancel handler to main thread correctly.  Running on thread ${Thread.currentThread()}")
                    }
                    networkTask.cancel()
                }
            }
            subscriber.onSubscribe(subscription)
            networkTask.resume()
        }
    }
}

// TODO fun <T> Publisher<T>.observeOn() { }
