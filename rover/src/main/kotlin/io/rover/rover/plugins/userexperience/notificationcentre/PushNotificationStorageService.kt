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
import io.rover.rover.platform.LocalStorage
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.PushNotification
import io.rover.rover.plugins.data.graphql.getObjectIterable
import io.rover.rover.plugins.data.graphql.operations.data.decodeJson
import io.rover.rover.plugins.data.graphql.operations.data.encodeJson
import org.json.JSONArray
import org.json.JSONException

class PushNotificationStorageService(
    private val dataPlugin: DataPluginInterface,
    localStorage: LocalStorage
): PushNotificationStorageServiceInterface {

    override fun getNotifications(): Publisher<List<PushNotification>> = Observable.merge(
        Observable.just(currentNotificationsOnDisk()).filterNulls(),
        epic.share()
    ).doOnSubscribe {
        actions.onNext(Action.Refresh())
    }

    private val keyValueStorage = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)
    // will subscribe to pushes and keep itself up to date

    // will pull updates from data plugin (devicestate) on demand


    private val actions = PublishSubject<Action>()

    private fun latestNotificationsFromCloud(): Publisher<List<PushNotification>> {
        return { callback: CallbackReceiver<NetworkResult<DeviceState>> ->  dataPlugin.fetchStateTask(callback) }.asPublisher().flatMap { networkResult ->
            when(networkResult) {
                is NetworkResult.Error -> throw(Exception("Unable to load notifications from DeviceState from the Rover API.", networkResult.throwable))
                is NetworkResult.Success -> Observable.just(networkResult.response.notifications)
            }
        }
    }

    private fun currentNotificationsOnDisk(): List<PushNotification>? {
        return try {
            JSONArray(keyValueStorage[STORE_KEY]).getObjectIterable().map { notificationJson ->
                PushNotification.decodeJson(notificationJson)
            }
        } catch (e: JSONException) {
            log.e("Invalid JSON appeared in Notifications cache: ${e.message}")
            null
        }
    }

    private fun updateLocalStorageWith(notifications: List<PushNotification>) {
        keyValueStorage[STORE_KEY] = JSONArray(notifications.map { it.encodeJson() }).toString()
    }

    private val epic = actions.flatMap { action ->
        when(action) {
            is Action.Refresh -> {
                latestNotificationsFromCloud().doOnNext { newNotifications ->
                    // update local storage!
                    updateLocalStorageWith(newNotifications)
                }
            }
        }
    }.share()



    // observe notifications coming in from events and notifications arriving from cloud.  how do we
    // trigger cloud updates? subscriptions! however, same problem all over again: difficult to do
    // singleton side-effects of a chain that involves an on-subscription side-effect.

    //

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.notification-storage"
        private const val STORE_KEY = "local-notifications-cache"
    }

    sealed class Action {
        class Refresh: Action()
    }
}
