package io.rover.rover.core.events

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.rover.rover.core.logging.log
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.domain.Context
import io.rover.rover.core.data.domain.EventSnapshot
import io.rover.rover.core.data.graphql.getObjectIterable
import io.rover.rover.core.data.graphql.operations.data.asJson
import io.rover.rover.core.data.graphql.operations.data.decodeJson
import io.rover.rover.core.events.domain.Event
import org.json.JSONArray
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.Executors


class EventQueueService(
    private val eventsPluginComponents: EventsPluginComponentsInterface,
    private val flushAt: Int,
    private val flushIntervalSeconds: Double,
    private val maxBatchSize: Int,
    private val maxQueueSize: Int
) : EventQueueServiceInterface {
    private val serialQueueExecutor = Executors.newSingleThreadExecutor()
    private val contextProviders: MutableList<ContextProvider> = mutableListOf()
    private val keyValueStorage = eventsPluginComponents.localStorage.getKeyValueStorageFor(Companion.STORAGE_CONTEXT_IDENTIFIER)

    // state:
    private val eventQueue: Deque<EventSnapshot> = LinkedList()
    private var context: Context? = null
    private var isFlushingEvents: Boolean = false

    override fun addContextProvider(contextProvider: ContextProvider) {
        contextProviders.add(contextProvider)
    }

    override fun trackEvent(event: Event) {
        log.v("Tracking event: $event")
        captureContext()
        enqueueEvent(event)
        flushEvents(flushAt)
    }

    override fun flushNow() {
        flushEvents(1)
    }

    private fun enqueueEvent(event: Event) {
        serialQueueExecutor.execute {
            if(eventQueue.count() == maxQueueSize) {
                log.w("Event queue is at capacity ($maxQueueSize) -- removing oldest event.")
                eventQueue.removeFirst()
            }

            val snapshot = EventSnapshot.fromEvent(
                event,
                context ?: throw RuntimeException("enqueueEvent() occurred before Context set up?")
            )
            eventQueue.add(snapshot)
            persistEvents()
        }
    }

    private fun persistEvents() {
        serialQueueExecutor.execute {
            val json = JSONArray(eventQueue.map { event -> event.asJson(eventsPluginComponents.dateFormatting) }).toString(4)
            keyValueStorage.set(Companion.QUEUE_KEY, json)
        }
    }

    private fun restoreEvents() {
        // load the current events from key value storage.

        // I will borrow the JSON serialization extension methods from the Data Plugin.  Not exactly
        // optimal, but it will do for now.
        eventQueue.clear()

        val storedJson = keyValueStorage.get(Companion.QUEUE_KEY)

        if(storedJson != null) {
            val decoded = try {
                JSONArray(storedJson).getObjectIterable().map { jsonObject ->
                    EventSnapshot.decodeJson(jsonObject, eventsPluginComponents.dateFormatting)
                }
            } catch (e: Throwable) {
                log.w("Invalid persisted events queue.  Ignoring and starting fresh. ${e.message}")
                null
            }

            eventQueue.addAll(
                decoded ?: emptyList()
            )

            if(eventQueue.isNotEmpty()) {
                log.v("Events queue with ${eventQueue.count()} events waiting has been restored.")
            }
        }
    }

    private fun flushEvents(minBatchSize: Int) {
        serialQueueExecutor.execute {
            if(isFlushingEvents) {
                log.w("Skipping flush, already in progress")
                return@execute
            }
            if(eventQueue.isEmpty()) {
                log.v("Skipping flush -- no events in the queue.")
                return@execute
            }
            if(eventQueue.count() < minBatchSize) {
                log.v("Skipping flush -- less than $minBatchSize events in the queue.")
                return@execute
            }

            val events = eventQueue.take(maxBatchSize)
            log.v("Uploading ${events.count()} event(s) to the Rover API.")

            isFlushingEvents = true

            eventsPluginComponents.dataPlugin.sendEventsTask(events) { networkResult ->
                when(networkResult) {
                    is NetworkResult.Error -> {
                        log.w("Error delivering ${events.count()} to the Rover API: ${networkResult.throwable.message}")

                        if(networkResult.shouldRetry) {
                            log.w("... will leave them enqueued for a future retry.")
                        } else {
                            removeEvents(events)
                        }
                    }
                    is NetworkResult.Success -> {
                        log.v("Successfully uploaded ${events.count()} events.")
                        removeEvents(events)
                    }
                }
                isFlushingEvents = false
            }.resume()
        }
    }

    private fun removeEvents(eventsToRemove: List<EventSnapshot>) {
        val idsToRemove = eventsToRemove.associateBy { it.id }
        serialQueueExecutor.execute {
            eventQueue.clear()
            eventQueue.addAll(
                eventQueue.filter { existingEvent ->
                    !idsToRemove.containsKey(existingEvent.id)
                }
            )
            log.v("Removed ${eventsToRemove.count()} event(s) from the queue -- it now contains ${eventQueue.count()} event(s).")
            persistEvents()
        }
    }

    private fun captureContext() {
        serialQueueExecutor.execute {
            log.v("Capturing context...")
            context = contextProviders.fold(Context.blank()) { current, provider ->
                provider.captureContext(current)
            }
            log.v("Context is now: $context.")
        }
    }

    override fun setPushToken(token: String?) {
        (contextProviders.first { it is PushTokenTransmissionChannel } as PushTokenTransmissionChannel).setPushToken(token)
    }

    init {
        restoreEvents()

        log.v("Starting $flushIntervalSeconds timer for submitting events.")

        // run a timer
        val handler = Handler(Looper.getMainLooper())
        fun scheduleFlushPoll() {
            handler.postDelayed({
                flushEvents(1)
                scheduleFlushPoll()
            }, flushIntervalSeconds.toLong() * 1000)
        }
        scheduleFlushPoll()

        // TODO: wire up Application-level activity callbacks after all to flush queue whenever an activity pauses.
        eventsPluginComponents.application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity?) {
                    log.d("An Activity is pausing, flushing Rover events queue.")
                    flushNow()
                }

                override fun onActivityResumed(activity: Activity?) { }

                override fun onActivityStarted(activity: Activity?) {  }

                override fun onActivityDestroyed(activity: Activity?) { }

                override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) { }

                override fun onActivityStopped(activity: Activity?) { }

                override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) { }
            }
        )
    }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.events-queue"
        private const val QUEUE_KEY = "queue"
    }
}
