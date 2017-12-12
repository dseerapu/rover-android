package io.rover.rover.streams

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.view.View
import io.rover.rover.services.network.NetworkTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

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

                        if(remainingSources.isEmpty()) {
                            subscriber.onComplete()
                        } else {
                            // subscribe to the source:
                            remainingSources.first().subscribe(object : Subscriber<T> {
                                override fun onComplete() {
                                    // While there is potentially a risk of a stack overflow here,
                                    // but in practical terms, there will not be that many sources
                                    // given to concat().
                                    if(!cancelled) recursiveSubscribe(
                                        remainingSources.subList(1, remainingSources.size)
                                    )
                                }

                                override fun onError(error: Throwable) {
                                    if(!cancelled) subscriber.onError(error)
                                }

                                override fun onNext(item: T) {
                                    if(!cancelled) subscriber.onNext(item)
                                }

                                override fun onSubscribe(subscription: Subscription) { /* no-op: we don't tell our subscribers about each source subscribing */ }
                            })
                        }
                    }

                    recursiveSubscribe(sources.asList())
                }
            }
        }
    }
}

interface Processor<T, R>: Subscriber<T>, Publisher<R>

typealias Observable<T> = Publisher<T>

fun <T> Publisher<T>.subscribe(onNext: (item: T) -> Unit, onError: (throwable: Throwable) -> Unit) {
    this.subscribe(object: Subscriber<T> {
        override fun onComplete() { }

        override fun onError(error: Throwable) { onError(error) }

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
            val outstanding : ConcurrentHashMap<Subscriber<*>, Boolean> = ConcurrentHashMap()

            fun informSubscriberCompleteIfAllCompleted() {
                // TODO: wait for all waiting transform subscriptions to complete too.
                if(outstanding.isEmpty()) {
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

                    val transformSubscriber =  object : Subscriber<R> {
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
 * Note that [share] will immediately subscribe (unlike the implementation in RxJava), so if you use
 * it on a cold [Publisher] (one that yields items on subscribe right away), you will almost
 * certainly miss those items.
 *
 */
fun <T> Publisher<T>.share(): Publisher<T> {
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
            }

            override fun onSubscribe(subscription: Subscription) { /* no-op */ }
        }
    )

    // subscribe right away and multicast to any listeners
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
        }
    }
}

interface Subject<T>: Processor<T, T>

/**
 *
 */
class PublishSubject<T> : Subject<T> {
    var subscriber : Subscriber<T>? = null

    override fun subscribe(subscriber: Subscriber<T>) {
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
    class Attach: ViewEvent()
    class Detach: ViewEvent()
}

/**
 * Observe attach and detach events from the given Android [View].
 */
fun View.attachEvents(): Publisher<ViewEvent> {
    return object : Publisher<ViewEvent> {
        override fun subscribe(subscriber: Subscriber<ViewEvent>) {
            val listener = object :  View.OnAttachStateChangeListener {
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
    return object: Publisher<Lifecycle.Event> {
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
typealias  CallbackReceiver<T> = (T) -> Unit


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
            val networkTask = this@asPublisher.invoke { result ->
                subscriber.onNext(result)
                subscriber.onComplete()
            }
            val subscription = object : Subscription {
                override fun cancel() {
                    networkTask.cancel()
                }
            }
            networkTask.resume()
        }
    }
}


// TODO fun <T> Publisher<T>.observeOn() { }
