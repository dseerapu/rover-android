package io.rover.rover.notifications

import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import io.rover.rover.core.data.domain.Context
import io.rover.rover.core.events.ContextProvider

/**
 * Will identify if the user has disabled notifications from the app.
 *
 * Note that on Android they are enabled by default.
 */
class NotificationContextProvider(
    private val applicationContext: android.content.Context
): ContextProvider {
    override fun captureContext(context: Context): Context {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        return context.copy(
            notificationAuthorization = when(notificationManager.areNotificationsEnabled()) {
                true -> Context.NotificationAuthorization.Authorized
                false -> Context.NotificationAuthorization.Denied
            }
        )
    }
}