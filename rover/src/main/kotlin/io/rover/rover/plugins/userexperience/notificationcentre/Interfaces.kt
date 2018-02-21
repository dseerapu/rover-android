package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.Publisher
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel

/**
 * Responsible for keeping a local cache of received push notifications, in addition to refreshing
 * them from [DeviceState] in the Data plugin from time to time.
 */
interface NotificationCenterStoreInterface {
    /**
     * Obtain the list of push notifications received by this device (and that were marked for
     * storage in the Notification Center).
     *
     * TODO: should this just be a callback version?
     */
    fun getNotifications(): Publisher<List<Notification>>
}

interface NotificationCenterViewModelInterface: BindableViewModel {
    // TODO only event here is a notification list update, at least for now!

    // what about an error event? snackbar time?

    // customization of error behaviour?

    // generation of "notification row" view models?

    // recycler behaviour? in this case we'll just be using the standard linear recycler manager

    // a little bit different from ScreenViewModel; the list will appear asynchronously.
    fun events(): Observable<Event>

    sealed class Event {
        class ListReady(
            val listViewModel: NotificationCenterListViewModelInterface
        ): Event()

        class DisplayProblemMessage(
            // TODO this will need to be configurable by the customer.
            reason: String
        ): Event()
    }
}

interface NotificationCenterListViewModelInterface: BindableViewModel {
    // so we gotta support basically arbitrary views.  Probably bend MVVM best-practice and just
    // pass the Notification model object through.


    /**
     * The notifications the view should show.  When the list is empty, the view may display an
     * empty list.
     *
     * Note: unusually for an MVVM UI pattern, this is exposing the [Notification] domain model
     * object.
     *
     * This is to better suit View implementations that may display any arbitrary detail of the
     * Notification.
     */
    val listOfNotifications: List<Notification>

    // deletions
    // marking as read
    // opens


    fun events(): Observable<ItemSelectedEvent>


    sealed class Navigate {
        class OpenUrl
    }
    data class NotificationSelectedEvent(

    )

}

