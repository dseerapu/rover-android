package io.rover.rover.notifications.ui

import io.rover.rover.core.data.DataPluginInterface
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.notifications.NotificationHandlerInterface

/**
 * The notification centre allows you to embed a view of all previously received Rover pushes from
 * your campaigns in the app.
 *
 * It has some degree of default formatting, but it
 *
 * * depends on Data Plugin, Push Plugin, and Events Plugin
 *
 */
class NotificationCentre(
    private val dataPlugin: DataPluginInterface,
    private val notificationHandler: NotificationHandlerInterface,
    private val eventsPlugin: EventQueueServiceInterface
) {

}
