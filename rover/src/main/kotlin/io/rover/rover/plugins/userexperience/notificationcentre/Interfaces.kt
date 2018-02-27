package io.rover.rover.plugins.userexperience.notificationcentre

import android.os.Parcelable
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.Publisher
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel

/**
 * Responsible for keeping a local cache of received push notifications, in addition to refreshing
 * them from [DeviceState] in the Data plugin from time to time.
 */
interface NotificationsRepositoryInterface {
    /**
     * Obtain the list of push notifications received by this device (and that were marked for
     * storage in the Notification Center).
     *
     * A refresh is triggered when subscribed.
     *
     * TODO: should this just be a callback version?
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

    fun markRead(notification: Notification)

    fun delete(notification: Notification)

    sealed class Emission {
        sealed class Event: Emission() {
            data class Refreshing(val refreshing: Boolean): Event()
            data class FetchFailure(val reason: String): Event()
        }

        data class Update(val notifications: List<Notification>): Emission()
    }
}

//interface NotificationCenterViewModelInterface: BindableViewModel {
//    // TODO only event here is a notification list update, at least for now!
//
//    // what about an error event? snackbar time?
//
//    // customization of error behaviour?
//
//    // generation of "notification row" view models?
//
//    // recycler behaviour? in this case we'll just be using the standard linear recycler manager
//
//    // a little bit different from ScreenViewModel; the list will appear asynchronously.
//    fun events(): Observable<Event>
//
//    sealed class Event {
//        /**
//         *
//         */
//        class ListReady(
//            // val listViewModel: NotificationCenterListViewModelInterface
//
//        ): Event()
//
//        class DisplayProblemMessage(
//            // TODO this will need to be configurable by the customer.
//            reason: String
//        ): Event()
//    }
//}

interface NotificationCenterListViewModelInterface: BindableViewModel {
    // so we gotta support basically arbitrary views.  Probably bend MVVM best-practice and just
    // pass the Notification model object through.


//    /**
//     * The notifications the view should show.  When the list is empty, the view may display an
//     * empty list.
//     *
//     * Note: unusually for an MVVM UI pattern, this is exposing the [Notification] domain model
//     * object.
//     *
//     * This is to better suit View implementations that may display any arbitrary detail of the
//     * Notification.
//     */
//    val listOfNotifications: List<Notification>

    // deletions
    // marking as read
    // opens

    // issue events, and then the NotificationCenterViewModelInterface issues events to the store?
    // if this is the case, which makes sense, then there's one problem:
    // NotificationCenterListViewModelInterface loses its utility as a view model class that the
    // developer can use directly in order to opt out of the empty view/refresh. perhaps that wasn't
    // useful to begin with.

    // also, if all that is the case, then its not even worth it necessarily to have two view
    // models?  however, a single view model now would have the following concerns:

    // - fetching through NotificationStore
    // - async indication
    // - empty display
    // - switching to list view
    // - issuing delete/read messages in response to listview behaviours.

    // I think a separate list view model is appropriate (and what I've done elsewhere in the SDK),
    // just to manage complexity, but not to expand customization opportunities for the customer.

    // However, that does mean that I will need to emit deletion/mark-as-read events from here.
    // not the end of the world though.

    // ... although gone thought does occur to me: the approach of having updates being only for the
    // whole list. technically, recyclerview allows for updating the list granularly. so, replacing
    // and rebinding the list isn't strictly necessary.  I *could* have the containing view model
    // call the list-view with any changes? that allows to keep the separation, but jeeze, now
    // there's even more effing complication.

    // fuck it I'm going monolithic.

    // - swipe to left is delete

    // solutions all involve ItemTouchHelper (from recyclerview support lib)

    // which method am I going to use for rendering the red swipe-to-delete panel?
    // either just draw red and icon behind in swipe handler:
    //   - https://medium.com/@kitek/recyclerview-swipe-to-delete-easier-than-you-thought-cff67ff5e5f6
    //   - http://nemanjakovacevic.net/blog/english/2016/01/12/recyclerview-swipe-to-delete-no-3rd-party-lib-necessary/
    // OR another layout underneath each row item:
    //   -

    /**
     * Subscribe to this event stream to be informed of when a user performs an action that needs
     * to be handled.
     */
    fun events(): Observable<Event>

    // TODO: don't forget to merge Update/Event into same type graph under "Emission"

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

        sealed class Navigate {
            class OpenUrl: Navigate()

            class OpenExperience: Navigate()
        }

        /**
         * Th
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
