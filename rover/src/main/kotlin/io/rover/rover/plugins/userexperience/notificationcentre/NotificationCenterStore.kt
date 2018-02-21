package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.CallbackReceiver
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.doOnSubscribe
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.share
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

class NotificationCenterStore(
    private val dataPlugin: DataPluginInterface,
    private val dateFormatting: DateFormattingInterface,
    localStorage: LocalStorage
): NotificationCenterStoreInterface {

    // TODO: subscribe to pushes and file them into local cache

    // TODO: add methods for markAsRead and markAsDeleted.

    // TODO: change merge behaviour to handle both of those fields

    override fun getNotifications(): Publisher<List<Notification>> = Observable.merge(
        Observable.just(currentNotificationsOnDisk()).filterNulls(),
        epic.share()
    ).map { notifications ->
        notifications
            .filter { !it.deleted }
            .sortedBy { it.deliveredAt }
    }.doOnSubscribe {
        // trigger a refresh whenever a consumer subscribes.
        actions.onNext(Action.Refresh())
    }

    private val keyValueStorage = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)
    // will subscribe to pushes and keep itself up to date

    // will pull updates from data plugin (devicestate) on demand


    private val actions = PublishSubject<Action>()

    private fun latestNotificationsFromCloud(): Publisher<List<Notification>> {
        return { callback: CallbackReceiver<NetworkResult<DeviceState>> ->  dataPlugin.fetchStateTask(callback) }.asPublisher().flatMap { networkResult ->
            when(networkResult) {
                is NetworkResult.Error -> throw(Exception("Unable to load notifications from DeviceState from the Rover API.", networkResult.throwable))
                is NetworkResult.Success -> Observable.just(networkResult.response.notifications)
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
            log.e("Invalid JSON appeared in Notifications cache: ${e.message}")
            null
        }
    }

    private fun mergeLocalStorageWith(notifications: List<Notification>) {
        // TODO: rather than replacing, instead merge into local storage, enforcing the count limit
        // and respecting the existing Read bit (ie., maybe just don't replace existing items)

        // TODO For both `read` and `deleted` merge by ORing the two values together.  Otherwise
        // keep server versions of the notifications' contents.

        keyValueStorage[STORE_KEY] = JSONArray(notifications.map { it.encodeJson(dateFormatting) }).toString()
    }

    private val epic = actions.flatMap { action ->
        when(action) {
            is Action.Refresh -> {
                // TODO: instead of just calling mergeLocalStorageWith(), make it pure, use map, and
                // instead have a separate updateLocalStorage function.
                latestNotificationsFromCloud().doOnNext { newNotifications ->
                    // update local storage!
                    mergeLocalStorageWith(newNotifications)
                }
            }
        }
    }.share()

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
