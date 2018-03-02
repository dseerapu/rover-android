package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.CallbackReceiver
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.Scheduler
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.doOnSubscribe
import io.rover.rover.core.streams.filterForSubtype
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.observeOn
import io.rover.rover.core.streams.shareHotAndReplay
import io.rover.rover.core.streams.subscribeOn
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.whenNotNull
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.graphql.getObjectIterable
import io.rover.rover.plugins.data.graphql.operations.data.decodeJson
import io.rover.rover.plugins.data.graphql.operations.data.encodeJson
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.events.domain.Event
import org.json.JSONArray
import org.json.JSONException
import java.util.concurrent.Executor

class NotificationsRepository(
    private val dataPlugin: DataPluginInterface,
    private val dateFormatting: DateFormattingInterface,
    private val ioExecutor: Executor,
    private val mainThreadScheduler: Scheduler,
    private val eventsPlugin: EventsPluginInterface,
    localStorage: LocalStorage
): NotificationsRepositoryInterface {

    // TODO: receive and persist notifications inbound from actual pushes.

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

    private val keyValueStorage = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)
    // will subscribe to pushes and keep itself up to date

    // will pull updates from data plugin (devicestate) on demand

    private val actions = PublishSubject<Action>()

    sealed class CloudFetchResult {
        data class CouldNotFetch(val reason: String): CloudFetchResult()
        data class Succeeded(val notifications: List<Notification>): CloudFetchResult()
    }
    private fun latestNotificationsFromCloud(): Publisher<CloudFetchResult> {
        return { callback: CallbackReceiver<NetworkResult<DeviceState>> ->  dataPlugin.fetchStateTask(callback) }
            .asPublisher()
            .doOnSubscribe { log.v("Refreshing device state to obtain notifications list.") }
            .map { networkResult ->
                when(networkResult) {
                    is NetworkResult.Error -> {
                        CloudFetchResult.CouldNotFetch(networkResult.throwable.message ?: "Unknown")
                    }
                    is NetworkResult.Success -> CloudFetchResult.Succeeded(networkResult.response.notifications)
                }
            }.subscribeOn(ioExecutor)
    }

    private fun currentNotificationsOnDisk(): Publisher<List<Notification>?> {
        return Observable.defer {
                Observable.just(try {
                    keyValueStorage[STORE_KEY].whenNotNull { jsonString ->
                        JSONArray(jsonString).getObjectIterable().map { notificationJson ->
                            Notification.decodeJson(notificationJson, dateFormatting)
                        }
                    }
                } catch (e: JSONException) {
                    log.e("Invalid JSON appeared in Notifications cache, so starting fresh: ${e.message}")
                    null
                }
            )
        }.subscribeOn(ioExecutor)
    }

    private fun mergeWithLocalStorage(notificationsFromDeviceState: List<Notification>): Publisher<List<Notification>> {
        return currentNotificationsOnDisk().map { notifications ->
            val notificationsOnDiskById = notifications?.associateBy { it.id } ?: hashMapOf()
            // return the new notifications list, but OR with any existing records' isRead/isDeleted
            // state.

            notificationsFromDeviceState.map { newNotification ->
                notificationsOnDiskById[newNotification.id].whenNotNull {
                    newNotification.copy(
                        isRead = newNotification.isRead || it.isRead,
                        isDeleted = newNotification.isDeleted || it.isDeleted
                    )
                } ?: newNotification
            }.orderNotifications().take(MAX_NOTIFICATIONS_LIMIT)
        }.subscribeOn(ioExecutor)
    }

    // a rule: things that touch external stuff by I/O must be publishers.  flat functions are only
    // allowed if they are pure.

    private fun replaceLocalStorage(notifications: List<Notification>): Publisher<Unit> {
        return Observable.defer {
            log.v("Updating local storage with ${notifications.size} notifications containing: ${notifications}")
            keyValueStorage[STORE_KEY] = JSONArray(notifications.map { it.encodeJson(dateFormatting) }).toString()
            Observable.empty<Unit>()
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
                                    .doOnNext { notifications ->
                                        // side-effect: update local storage!
                                        replaceLocalStorage(notifications)
                                    }.map { NotificationsRepositoryInterface.Emission.Update(it) }
                            }
                        },
                    Observable.just(NotificationsRepositoryInterface.Emission.Event.Refreshing(false))
                )
            }
            is Action.MarkDeleted -> {
                doMarkAsDeleted(action.notification).map { NotificationsRepositoryInterface.Emission.Update(it) }
            }
            is Action.MarkRead -> {
                doMarkAsRead(action.notification).map {NotificationsRepositoryInterface.Emission.Update(it) }
            }
        }
    }.observeOn(mainThreadScheduler).shareHotAndReplay(0) // TODO: using observeOnAndroidMainThread in its current form will be problematic for tests.

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
                    )
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

            val alreadyRead = onDisk.find { it.id == notification.id }?.isDeleted ?: false

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
                    )
                )
            }

            replaceLocalStorage(
                modified
            ).map { modified }
        }.subscribeOn(ioExecutor)
    }

    private fun List<Notification>.orderNotifications(): List<Notification> {
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
    }
}
