package io.rover.rover.plugins.push

import android.content.Intent
import android.os.Bundle
import io.rover.rover.plugins.data.domain.PushNotificationAction

interface PushPluginInterface {
    /**
     * You need to implement a
     * [FirebaseMessagingService](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService)
     * in your application, and then override its `onMessageReceived` method.
     *
     * If you are using GCM instead of FCM, then look at [onMessageReceivedDataAsBundle] instead.
     *
     * Then, retrieve `data` from the `RemoteMessage` object it received and pass it here.
     *
     * In response, the Push Plugin will build an appropriate notification and add it to the Android
     * notification area (although note this will not be called for every notification sent to users
     * on behalf of your application; if the Rover Cloud determined that a Firebase Display Message
     * was sufficient to display the push, then this callback may not happen at all unless the app
     * is in the foreground).
     *
     * In Kotlin, it may look something like this:
     *
     * ```
     * class MyAppCustomFirebaseReceiver: FirebaseMessagingService() {
     *     override fun onMessageReceived(remoteMessage: RemoteMessage) {
     *         val pushPlugin = Rover.sharedInstance.pushPlugin
     *         pushPlugin.onMessageReceivedData(remoteMessage.data)
     *     }
     * }
     * ```
     */
    fun onMessageReceivedData(parameters: Map<String, String>)

    /**
     * Equivalent to [onMessageReceivedData], but accepts a Bundle instead of a Map.
     *
     * This version is appropriate for use with GCM in lieu of FCM.  See `README.legacy-gcm.md` for
     * details.
     */
    fun onMessageReceivedDataAsBundle(parameters: Bundle)

    /**
     * You need to implement a
     * [FirebaseInstanceIdService](https://firebase.google.com/docs/reference/android/com/google/firebase/iid/FirebaseInstanceIdService)
     * in your application, and then override its `onTokenRefresh` method.
     *
     * Then, pass the token it received here.  Thread safe; you may call this from whatever thread
     * you like.
     *
     * Then the Rover SDK will be able to register that push token to receive Rover pushes.  If this
     * step is omitted, then the application will never receive any Rover-powered push
     * notifications.
     */
    fun onTokenRefresh(token: String?)
}

interface NotificationActionRoutingBehaviourInterface {
    /**
     * Should return an Intent for the given push notification action.
     */
    fun notificationActionToIntent(action: PushNotificationAction): Intent
}
