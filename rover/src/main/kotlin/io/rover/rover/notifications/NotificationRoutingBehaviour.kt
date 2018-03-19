package io.rover.rover.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.TaskStackBuilder
import io.rover.rover.core.data.domain.Notification
import io.rover.rover.core.data.domain.PushNotificationAction
import io.rover.rover.core.logging.log
import io.rover.rover.experiences.TopLevelNavigation
import java.net.URI

/**
 *
 */
open class NotificationActionRoutingBehaviour(
    private val applicationContext: Context,
    private val topLevelNavigation: TopLevelNavigation,

    // TODO will need a postfix to be passed in for the `rv-` prefix
    /**
     * Rover deep links are customized for each app in this way:
     *
     * rv-myapp://...
     *
     * You must select an appropriate slug without spaces or special characters to be used in place
     * of `myapp` above.  You must also configure this in your Rover settings TODO explain how
     *
     * You should also consider adding the handler to the manifest.  While this is not needed for
     * any Rover functionality to work, it is required for clickable deep/universal links to work from
     * anywhere else. TODO explain how once the stuff to do so is built
     */
    private val deepLinkSchemaSlug: String
): NotificationActionRoutingBehaviourInterface {

    init {
        // validate the deep link slug and ensure it's sane.

        when {
            deepLinkSchemaSlug.isBlank() -> throw RuntimeException("Deep link schema slug must not be blank.")
            deepLinkSchemaSlug.startsWith("rv-") -> throw RuntimeException("Do not include the `rv-` prefix do your deep link schema slug.  That is added for you.")
            deepLinkSchemaSlug.contains(" ") -> throw RuntimeException("Deep link schema slug must not contain spaces.")
            // TODO: check for special characters.
        }
    }

    private val fullSchema = "rv-$deepLinkSchemaSlug"

    override fun notificationActionToIntent(action: PushNotificationAction): Intent {
        return when(action.uri.scheme) {
            fullSchema -> {
                // one of our deeplinks
                when(action.uri.authority) {
                    "" -> {
                        // just open the app.
                        topLevelNavigation.openAppIntent()
                    }
                    "website" -> {
                        // open a website, TODO in the hosted chrome thingy.

                        // TODO crap gotta parse the query params.

                        val websiteUri = action.uri.query.parseAsQueryParameters()["website"]

                        if(websiteUri.isNullOrBlank()) {
                            log.w("Website URI missing from present website deep link.  Was ${action.uri}")
                            // just default to opening the app in this case.
                            topLevelNavigation.openAppIntent()
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse(websiteUri))
                        }
                    }
                    "experience" -> {
                        val queryParams = action.uri.query.parseAsQueryParameters()

                        val experienceId = queryParams["id"]
                        val campaignId = queryParams["campaignId"]

                        if(experienceId == null || experienceId.isBlank()) {
                            log.w("Experience ID missing from present experience deep link.")
                            // just default to opening the app in this case.
                            topLevelNavigation.openAppIntent()
                        } else {
                            topLevelNavigation.displayExperienceIntent(experienceId, campaignId)
                        }
                    }
                    else -> {
                        log.w("Unknown authority given in deep link: ${action.uri}")
                        // just default to opening the app in this case.
                        topLevelNavigation.openAppIntent()
                    }
                }
            }
            else -> {
                // an external link, either web or another deep link.  Hand over to Android's
                // builtin Intent routing system.
                Intent(Intent.ACTION_VIEW, Uri.parse(action.uri.toString()))
            }
        }
    }

    private fun String.parseAsQueryParameters(): Map<String, String> {
        return split("&").map {
            val keyAndValue = it.split("=")
            Pair(keyAndValue.first(), keyAndValue[1])
        }.associate { it }
    }
}

interface NotificationContentPendingIntentSynthesizerInterface {
    fun synthesizeNotificationIntentStack(notification: Notification): List<Intent>
}

class NotificationContentPendingIntentSynthesizer(
    private val applicationContext: Context,
    private val topLevelNavigation: TopLevelNavigation,
    private val notificationActionRoutingBehaviour: NotificationActionRoutingBehaviourInterface
): NotificationContentPendingIntentSynthesizerInterface {
    override fun synthesizeNotificationIntentStack(notification: Notification): List<Intent> {
        val targetIntent = notificationActionRoutingBehaviour.notificationActionToIntent(notification.action)

        // now to synthesize the backstack.
        return TaskStackBuilder.create(applicationContext).apply {
            if(notification.isNotificationCenterEnabled) {
                // inject the Notification Centre for the user's app. TODO: allow user to *configure*
                // what their notification centre is, either with a custom URI template method OR
                // just with a meta-property in their Manifest. but by default we can bundle an Activity that hosts NotificationCentreView, I think.

                // for now, we'll just put some sort of
                addNextIntent(topLevelNavigation.displayNotificationCenterIntent())
            } else {
                // Instead of displaying the notification centre, display the parent activity the user set
                addNextIntent(topLevelNavigation.openAppIntent())
            }

            // so, targetIntent, since it uses extra data to pass arguments, might be a problem:
            // PendingIntents are value objects, but they do not fully encapsulate any extras data,
            // so they may find themselves "merged".  However, perhaps TaskStackBuilder is handling
            // this problem.
            addNextIntent(targetIntent)
        }.intents.asList().apply {
            this.first().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
            this.last().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // .intents.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)!!
    }
}