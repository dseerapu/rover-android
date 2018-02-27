package io.rover.rover.plugins.userexperience

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.rover.rover.plugins.userexperience.experience.containers.StandaloneExperienceHostActivity

/**
 * A default version of [TopLevelNavigation] that will use the two Activities bundled with the Rover
 * SDK for displaying Experiences and the Notification Center.
 */
open class DefaultTopLevelNavigation(
    private val applicationContext: Context
): TopLevelNavigation {
    override fun displayExperienceIntent(experienceId: String): Intent {
        return StandaloneExperienceHostActivity.makeIntent(applicationContext, experienceId)
    }

    override fun displayNotificationCenterIntent(): Intent {
        // TODO soon to be StandaloneNotificationCenterActivity
        return Intent(Intent.ACTION_VIEW, Uri.parse("about:blank"))
    }
}
