package io.rover.rover.plugins.userexperience.notificationcentre

import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.push.PushPluginInterface

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
    private val pushPlugin: PushPluginInterface,
    private val eventsPlugin: EventsPluginInterface
) {

}
