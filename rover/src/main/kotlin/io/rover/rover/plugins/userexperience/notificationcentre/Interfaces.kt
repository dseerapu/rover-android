package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.core.streams.Publisher
import io.rover.rover.plugins.data.domain.PushNotification
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel

/**
 * Responsible for keeping a local cache of received push notifications, in addition to refreshing
 * them from [DeviceState] in the Data plugin from time to time.
 */
interface PushNotificationStorageServiceInterface {
    /**
     * Obtain the list of push notifications received by this device (and that were marked for
     * storage in the Notification Center).
     *
     * TODO: should this just be a callback version?
     */
    fun getNotifications(): Publisher<List<PushNotification>>
}

interface NotificationCenterViewModelInterface: BindableViewModel {
    // TODO only event here is a notification list update, at least for now!

}
