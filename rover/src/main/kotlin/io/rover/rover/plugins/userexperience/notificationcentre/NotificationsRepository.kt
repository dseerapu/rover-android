package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.CallbackReceiver
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.doOnError
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.filter
import io.rover.rover.core.streams.filterForSubtype
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.share
import io.rover.rover.core.streams.shareHotAndReplay
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
import org.json.JSONArray
import org.json.JSONException

class NotificationsRepository(
    private val dataPlugin: DataPluginInterface,
    private val dateFormatting: DateFormattingInterface,
    localStorage: LocalStorage
): NotificationsRepositoryInterface {

    // TODO: subscribe to pushes and file them into local cache

    // TODO: add methods for markAsRead and markAsDeleted.

    // TODO: change merge behaviour to handle both of those fields

    override fun updates(): Publisher<NotificationsRepositoryInterface.Emission.Update> = Observable.merge(
        Observable.just(currentNotificationsOnDisk()).filterNulls().map { existingNotifications ->
            NotificationsRepositoryInterface.Emission.Update(existingNotifications)
        },
        epic
    ).filterForSubtype<NotificationsRepositoryInterface.Emission.Update, NotificationsRepositoryInterface.Emission>().shareHotAndReplay(1)

    override fun events(): Publisher<NotificationsRepositoryInterface.Emission.Event> = epic.filterForSubtype()

    override fun refresh() {
        log.v("Dispatching refresh request")
        actions.onNext(Action.Refresh())
    }

    override fun markRead(notification: Notification) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(notification: Notification) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        return { callback: CallbackReceiver<NetworkResult<DeviceState>> ->  dataPlugin.fetchStateTask(callback) }.asPublisher().map { networkResult ->
            when(networkResult) {
                is NetworkResult.Error -> CloudFetchResult.CouldNotFetch(networkResult.throwable.message ?: "Unknown")
                is NetworkResult.Success -> CloudFetchResult.Succeeded(networkResult.response.notifications)
            }
        }
    }

    private fun currentNotificationsOnDisk(): List<Notification>? {
        return try {
            keyValueStorage[STORE_KEY].whenNotNull { jsonString ->
                JSONArray(jsonString).getObjectIterable().map { notificationJson ->
                    Notification.decodeJson(notificationJson, dateFormatting)
                }
            }
        } catch (e: JSONException) {
            log.e("Invalid JSON appeared in Notifications cache, so starting fresh: ${e.message}")
            null
        }
    }

    private fun mergeLocalStorageWith(notifications: List<Notification>) {
        // TODO: rather than replacing, instead merge into local storage, enforcing the count limit
        // and respecting the existing Read bit (ie., maybe just don't replace existing items)

        // TODO For both `read` and `deleted` merge by ORing the two values together.  Otherwise
        // keep server versions of the notifications' contents.

        log.v("Updating local storage with ${notifications.size} notifications.")
        keyValueStorage[STORE_KEY] = JSONArray(notifications.map { it.encodeJson(dateFormatting) }).toString()
    }

    private val epic: Publisher<NotificationsRepositoryInterface.Emission> = actions.flatMap { action ->
        when(action) {
            is Action.Refresh -> {
                Observable.concat(
                    Observable.just(NotificationsRepositoryInterface.Emission.Event.Refreshing(true)),
                    latestNotificationsFromCloud()
                        .flatMap { fetchResult ->
                            when(fetchResult) {
                                is CloudFetchResult.CouldNotFetch -> Observable.just(NotificationsRepositoryInterface.Emission.Event.FetchFailure(fetchResult.reason))
                                is CloudFetchResult.Succeeded -> Observable.just(
                                    fetchResult.notifications
                                ).doOnNext {
                                    // TODO: instead of just calling mergeLocalStorageWith(), make it pure, use map, and
                                    // instead have a separate updateLocalStorage function.]

                                    // update local storage!
                                    mergeLocalStorageWith(it)
                                }.map { NotificationsRepositoryInterface.Emission.Update(it) }
                            }
                        },
                    Observable.just(NotificationsRepositoryInterface.Emission.Event.Refreshing(false))
                )
            }
        }
    }.share()

    private fun orderNotifications(notifications: List<Notification>): List<Notification> {
        return notifications
            .filter { !it.deleted }
            .sortedBy { it.deliveredAt }
    }

    // observe notifications coming in from events and notifications arriving from cloud.  how do we
    // trigger cloud updates? subscriptions! however, same problem all over again: difficult to do
    // singleton side-effects of a chain that involves an on-subscription side-effect.

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.notification-storage"
        private const val STORE_KEY = "local-notifications-cache"
    }

    sealed class Action {
        class Refresh: Action()
    }
}
