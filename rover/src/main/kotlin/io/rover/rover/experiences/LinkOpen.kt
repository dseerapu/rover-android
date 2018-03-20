package io.rover.rover.experiences

import android.content.Intent
import io.rover.rover.notifications.ActionRoutingBehaviourInterface
import java.net.URI

class LinkOpen(
    private val routingBehaviour: ActionRoutingBehaviourInterface
): LinkOpenInterface {
    override fun localIntentForReceived(receivedUri: URI): Intent {
        return routingBehaviour.notificationActionToIntent()
    }
}
