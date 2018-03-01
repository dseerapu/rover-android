package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.Publisher
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel

/**
 * This repository syncs and stores the Push Notifications.
 *
 * A per-device list of received push notifications is kept on the Rover Cloud API (although note
 * this is device-bound, and not bound to any sort of per-user account).  We just use SharedPreferences
 * to store a JSON-encoded blob of all of the current notifications.  This is sufficient because we
 * never anticipate storing more than 100.
 *
 * The arrangement is rather CQRS-like; the notifications list coming from the cloud API is
 * read-only, any items in it can only be marked as read or deleted (the only possible mutations)
 * not through the GraphQL API but instead only by asynchronously emitted events that may be applied
 * any arbitrary amount of time in the future.  This Repository is responsible for maintaining its
 * own state for read and deleted.
 */
interface NotificationsRepositoryInterface {
    /**
     * Obtain the list of push notifications received by this device (and that were marked for
     * storage in the Notification Center).
     *
     * A refresh is triggered when subscribed.
     */
    fun updates(): Publisher<Emission.Update>

    /**
     * Be informed of changes occurring to the Repository itself; for instance, if a refresh cycle
     * currently running.
     */
    fun events(): Publisher<Emission.Event>

    /**
     * Manually trigger a refresh.
     */
    fun refresh()

    /**
     * Request that the notification be as marked as read.  This method will return immediately.
     * The consumer will see the changes by a new [Emission.Update] being updated on the [updates]
     * publisher.
     */
    fun markRead(notification: Notification)

    /**
     * Request that the notification be marked as deleted.  This method will return immediately.
     * The consumer will see the changes by a new [Emission.Update] being updated on the [updates]
     * publisher.
     */
    fun delete(notification: Notification)

    sealed class Emission {
        sealed class Event: Emission() {
            data class Refreshing(val refreshing: Boolean): Event()
            data class FetchFailure(val reason: String): Event()
        }

        data class Update(val notifications: List<Notification>): Emission()
    }
}


interface NotificationCenterListViewModelInterface: BindableViewModel {
    /**
     * Subscribe to this event stream to be informed of when a user performs an action that needs
     * to be handled.
     */
    fun events(): Observable<Event>

    sealed class Event {
        /**
         * The list has changed.
         *
         * Note that the IDs of notifications themselves are guaranteed to be stable, so they should
         * be used to perform a differential update (which RecyclerView supports).
         *
         * If the list is empty, display the empty view.
         *
         * Note: unusually for an MVVM UI pattern, this is exposing the [Notification] domain model
         * object directly.
         *
         * This is to better suit View implementations that may display any arbitrary detail of the
         * Notification.  Notification itself is a value object.
        */
        data class ListUpdated(val notifications: List<Notification>): Event()

        data class Navigate(val action: PushNotificationAction): Event()

        /**
         * The backing data store is in the process of starting or stopping a refresh operation. The
         * consumer may use this event to indicate that a refresh is running.
         */
        data class Refreshing(val refreshing: Boolean): Event()

        class DisplayProblemMessage: Event()
    }

    /**
     * Emit the appropriate Event needed for this notification being clicked.
     */
    fun notificationClicked(notification: Notification)

    /**
     * Attempt to delete the notification from the Rover cloud device state API, and also
     * mark it as deleted in the local store.
     */
    fun deleteNotification(notification: Notification)

    /**
     * User did the pull down gesture to ask for a refresh.
     */
    fun requestRefresh()
}
