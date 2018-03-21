package io.rover.rover.experiences

import android.content.Intent
import io.rover.rover.notifications.ActionRoutingBehaviourInterface
import io.rover.rover.notifications.NotificationContentPendingIntentSynthesizerInterface
import java.net.URI

class LinkOpen(
    private val routingBehaviour: ActionRoutingBehaviourInterface,
    private val synthesizerInterface: NotificationContentPendingIntentSynthesizerInterface,
    private val topLevelNavigation: TopLevelNavigation
): LinkOpenInterface {
    override fun localIntentForReceived(receivedUri: URI): List<Intent> {
        return if(receivedUri.scheme == "https" || receivedUri.scheme == "http") {
            // so, if it is an http or https, we can assume it is a Rover Experience universal link.

            synthesizerInterface.synthesizeNotificationIntentStack(
                topLevelNavigation.displayExperienceIntent(receivedUri),
                false
            )
        } else {
            val action = routingBehaviour.actionUriToIntent(receivedUri)

            if(action.noBackstack) {
                // no backstack
                listOf(action.intent)
            } else {
                synthesizerInterface.synthesizeNotificationIntentStack(
                    action.intent,
                    false
                )
            }
        }
    }
}
