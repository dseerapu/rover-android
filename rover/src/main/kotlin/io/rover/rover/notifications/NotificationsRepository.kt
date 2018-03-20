package io.rover.rover.notifications

import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.*
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.whenNotNull
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.domain.DeviceState
import io.rover.rover.core.data.domain.Notification
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.data.graphql.getObjectIterable
import io.rover.rover.core.data.graphql.operations.data.decodeJson
import io.rover.rover.core.data.graphql.operations.data.encodeJson
import io.rover.rover.core.events.EventQueueService
import io.rover.rover.notifications.ui.NotificationsRepositoryInterface
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.events.domain.Event
import io.rover.rover.platform.merge
import org.json.JSONArray
import org.json.JSONException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Responsible for reactively persisting notifications and informing subscribers of changes.
 *
 * Must be a singleton, because changes dispatch updates to all subscribers as a side-effect.
 */
class NotificationsRepository(
    private val graphQlApiService: GraphQlApiServiceInterface,
    private val dateFormatting: DateFormattingInterface,
    private val ioExecutor: Executor,
    mainThreadScheduler: Scheduler,
    private val eventsPlugin: EventQueueServiceInterface,
    localStorage: LocalStorage
): NotificationsRepositoryInterface {

    // TODO: receive and persist notifications inbound from actual pushes.

    // TODO: gate access to the localStorage via a single-thread executor pool.

    override fun updates(): Publisher<NotificationsRepositoryInterface.Emission.Update> = Observable.concat(
        currentNotificationsOnDisk().filterNulls().map { existingNotifications ->
            NotificationsRepositoryInterface.Emission.Update(existingNotifications)
        },
        epic
    ).filterForSubtype<NotificationsRepositoryInterface.Emission.Update, NotificationsRepositoryInterface.Emission>()

    override fun events(): Publisher<NotificationsRepositoryInterface.Emission.Event> = epic.filterForSubtype()

    override fun refresh() {
        actions.onNext(Action.Refresh())
    }

    override fun markRead(notification: Notification) {
        actions.onNext(Action.MarkRead(notification))
    }

    override fun delete(notification: Notification) {
        actions.onNext(Action.MarkDeleted(notification))
    }

    override fun notificationArrivedByPush(notification: Notification) {
        actions.onNext(Action.NotificationArrivedByPush(notification))
    }

    private val keyValueStorage = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)

    private val actions = PublishSubject<Action>()

    sealed class CloudFetchResult {
        data class CouldNotFetch(val reason: String): CloudFetchResult()
        data class Succeeded(val notifications: List<Notification>): CloudFetchResult()
    }
    private fun latestNotificationsFromCloud(): Publisher<CloudFetchResult> {
        return { callback: CallbackReceiver<NetworkResult<DeviceState>> ->  graphQlApiService.fetchStateTask(callback) }
            .asPublisher()
            .doOnSubscribe { log.v("Refreshing device state to obtain notifications list.") }
            .map { networkResult ->
                when(networkResult) {
                    is NetworkResult.Error -> {
                        CloudFetchResult.CouldNotFetch(networkResult.throwable.message
                            ?: "Unknown")
                    }
                    is NetworkResult.Success -> CloudFetchResult.Succeeded(networkResult.response.notifications)
                }
            }.subscribeOn(ioExecutor)
    }

    private fun currentNotificationsOnDisk(): Publisher<List<Notification>?> {
        return Observable.defer {
                Observable.just(
                    try {
                        keyValueStorage[STORE_KEY].whenNotNull { jsonString ->
                            JSONArray(jsonString).getObjectIterable().map { notificationJson ->
                                Notification.decodeJson(notificationJson, dateFormatting)
                            }
                        }
                    } catch (e: JSONException) {
                        log.w("Invalid JSON appeared in Notifications cache, so starting fresh: ${e.message}")
                        null
                    }
                )
        }.subscribeOn(ioExecutor)
    }

    /**
     * Add the existing notifications on disk together any new ones.
     *
     * The new list need not be exhaustive; existing non-conflicting records will be kept,
     * up until a cutoff.  That means this method may be used for partial updates.
     */
    private fun mergeWithLocalStorage(incomingNotifications: List<Notification>): Publisher<List<Notification>> {
        return currentNotificationsOnDisk().map { notifications ->

            val notificationsOnDiskById = notifications?.associateBy { it.id } ?: hashMapOf()
            val incomingNotificationsById = incomingNotifications.associateBy { it.id }

            notificationsOnDiskById.merge(incomingNotificationsById) { existing, incoming ->
                incoming.copy(
                    isRead = incoming.isRead || existing.isRead,
                    isDeleted = incoming.isDeleted || existing.isDeleted
                )
            }.values.orderNotifications().take(MAX_NOTIFICATIONS_LIMIT)
        }.subscribeOn(ioExecutor).doOnNext { log.v("Merge result: $it") }
    }

    // a rule: things that touch external stuff by I/O must be publishers.  flat functions are only
    // allowed if they are pure.

    private fun replaceLocalStorage(notifications: List<Notification>): Publisher<List<Notification>> {
        return Observable.defer {
            log.v("Updating local storage with ${notifications.size} notifications containing: ${notifications}")
            keyValueStorage[STORE_KEY] = JSONArray(notifications.map { it.encodeJson(dateFormatting) }).toString()
            Observable.just(notifications)
        }.subscribeOn(ioExecutor)
    }

    private val epic: Publisher<NotificationsRepositoryInterface.Emission> = actions.flatMap { action ->
        when(action) {
            is Action.Refresh -> {
                Observable.concat(
                    Observable.just(NotificationsRepositoryInterface.Emission.Event.Refreshing(true)),
                    latestNotificationsFromCloud()
                        .flatMap { fetchResult ->
                            when(fetchResult) {
                                is CloudFetchResult.CouldNotFetch -> Observable.just(
                                    NotificationsRepositoryInterface.Emission.Event.FetchFailure(fetchResult.reason)
                                )
                                is CloudFetchResult.Succeeded ->
                                    mergeWithLocalStorage(fetchResult.notifications)
                                    .flatMap { notifications ->
                                        // side-effect: update local storage!
                                        replaceLocalStorage(notifications).map {
                                            NotificationsRepositoryInterface.Emission.Update(it)
                                        }
                                    }
                            }
                        },
                    Observable.just(NotificationsRepositoryInterface.Emission.Event.Refreshing(false))
                )
            }
            is Action.MarkDeleted -> {
                doMarkAsDeleted(action.notification).map { NotificationsRepositoryInterface.Emission.Update(it) }
            }
            is Action.MarkRead -> {
                doMarkAsRead(action.notification).map { NotificationsRepositoryInterface.Emission.Update(it) }
            }
            is Action.NotificationArrivedByPush -> {
                mergeWithLocalStorage(listOf(action.notification)).flatMap { merged -> replaceLocalStorage(merged) }.map {
                    NotificationsRepositoryInterface.Emission.Update(it)
                }
            }
        }
    }.observeOn(mainThreadScheduler).shareHotAndReplay(0)

    /**
     * When subscribed, performs the side-effect of marking the given notification as deleted
     * locally (on the I/O executor).  If successful, it will yield an emission appropriate
     * to inform consumers of the change.
     */
    private fun doMarkAsDeleted(notification: Notification): Publisher<List<Notification>> {
        return currentNotificationsOnDisk().flatMap { onDisk ->
            if(onDisk == null) {
                log.w("No notifications currently stored on disk.  Cannot mark notification as deleted.")
                return@flatMap Observable.empty<List<Notification>>()
            }

            val alreadyDeleted = onDisk.find { it.id == notification.id }?.isDeleted ?: false

            val modified = onDisk.map { onDiskNotification ->
                if(onDiskNotification.id == notification.id) {
                    onDiskNotification.copy(isDeleted = true)
                } else onDiskNotification
            }

            if(!alreadyDeleted) {
                eventsPlugin.trackEvent(
                    Event(
                        "Notification Marked Deleted",
                        hashMapOf()
                    ),
                    EventQueueService.ROVER_NAMESPACE
                )
            }

            replaceLocalStorage(
                modified
            ).map { modified }
        }.subscribeOn(ioExecutor)
    }

    /**
     * When subscribed, performs the side-effect of marking the given notification as deleted
     * locally (on the I/O executor).
     */
    private fun doMarkAsRead(notification: Notification): Publisher<List<Notification>> {
        return currentNotificationsOnDisk().flatMap { onDisk ->
            if(onDisk == null) {
                log.w("No notifications currently stored on disk.  Cannot mark notification as read.")
                return@flatMap Observable.empty<List<Notification>>()
            }

            val alreadyRead = onDisk.find { it.id == notification.id }?.isRead ?: false

            val modified = onDisk.map { onDiskNotification ->
                if(onDiskNotification.id == notification.id) {
                    onDiskNotification.copy(isRead = true)
                } else onDiskNotification
            }

            if(!alreadyRead) {
                eventsPlugin.trackEvent(
                    Event(
                        "Notification Marked Read",
                        hashMapOf()
                    ),
                    EventQueueService.ROVER_NAMESPACE
                )
            }

            replaceLocalStorage(
                modified
            ).map { modified }
        }.subscribeOn(ioExecutor)
    }

    private fun Collection<Notification>.orderNotifications(): List<Notification> {
        return this
            .sortedByDescending { it.deliveredAt }
    }

    // observe notifications coming in from events and notifications arriving from cloud.  how do we
    // trigger cloud updates? subscriptions! however, same problem all over again: difficult to do
    // singleton side-effects of a chain that involves an on-subscription side-effect.

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.notification-storage"
        private const val STORE_KEY = "local-notifications-cache"

        private const val MAX_NOTIFICATIONS_LIMIT = 100
    }

    sealed class Action {
        /**
         * User has requested a refresh.
         */
        class Refresh: Action()

        /**
         * User has requested a refresh.
         */
        class MarkRead(val notification: Notification): Action()

        /**
         * User has requested a mark to be delete.
         */
        class MarkDeleted(val notification: Notification): Action()

        /**
         * A notification arrived by push.  This will add it to the repository.
         */
        class NotificationArrivedByPush(val notification: Notification): Action()
    }
}
