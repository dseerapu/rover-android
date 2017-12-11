package io.rover.rover.streams

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
                                    recursiveSubscribe(
                                        remainingSources.subList(1, remainingSources.size)
                                    )
                                }

                                override fun onError(error: Throwable) {
                                    subscriber.onError(error)
                                }

                                override fun onNext(item: T) {
                                    subscriber.onNext(item)
                                }

                                override fun onSubscribe(subscription: Subscription) { }
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

    override fun onSubscribe(subscription: Subscription) { /* no-op */ }

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

// TODO fun <T> Publisher<T>.observeOn() { }
