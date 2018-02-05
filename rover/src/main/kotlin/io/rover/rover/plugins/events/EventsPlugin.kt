package io.rover.rover.plugins.events

import android.os.Handler
import android.os.Looper
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.EventSnapshot
import io.rover.rover.plugins.data.graphql.getObjectIterable
import io.rover.rover.plugins.data.graphql.operations.data.asJson
import io.rover.rover.plugins.data.graphql.operations.data.decodeJson
import io.rover.rover.plugins.events.domain.Event
import org.json.JSONArray
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.Executors


class EventsPlugin(
    private val eventsPluginComponents: EventsPluginComponentsInterface,
    private val flushAt: Int,
    private val flushIntervalSeconds: Double,
    private val maxBatchSize: Int,
    private val maxQueueSize: Int
) : EventsPluginInterface {
    private val storageContextIdentifier = "io.rover.rover.events-queue"
    private val QUEUE_KEY = "queue"
    private val serialQueueExecutor = Executors.newSingleThreadExecutor()
    private val contextProviders: MutableList<ContextProviderInterface> = mutableListOf()
    private val keyValueStorage = eventsPluginComponents.localStorage.getKeyValueStorageFor(storageContextIdentifier)

    // state:
    private val eventQueue: Deque<EventSnapshot> = LinkedList()
    private var context: Context? = null
    private var isFlushingEvents: Boolean = false

    override fun addContextProvider(contextProvider: ContextProviderInterface) {
        contextProviders.add(contextProvider)
    }

    override fun trackEvent(event: Event) {
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
            keyValueStorage.set(QUEUE_KEY, json)
        }
    }

    private fun restoreEvents() {
        // load the current events from key value storage.

        // I will borrow the JSON serialization extension methods from the Data Plugin.  Not exactly
        // optimal, but it will do for now.
        eventQueue.clear()

        val storedJson = keyValueStorage.get(QUEUE_KEY)

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
            }
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
            log.v("Removed ${eventsToRemove.count()} event(s) from the queue -- it now contains ${eventsToRemove.count()} event(s).")
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
    }
}
