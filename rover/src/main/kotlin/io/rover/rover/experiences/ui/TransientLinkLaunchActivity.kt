package io.rover.rover.experiences.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.rover.rover.core.logging.log
import io.rover.rover.notifications.NotificationOpenInterface

/**
 * The intent filter you add for your universal links and `rv-$appname://` deep links should by default
 * point to this activity.
 */
open class TransientLinkLaunchActivity : AppCompatActivity() {
    // TODO: this needs to be some sort of LinkOpenInterface.  It would still need to use
    // notification action routing behaviour, but

    // private val notificationOpen: NotificationOpenInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.v("Transient link launch activity running.")
    }
}
