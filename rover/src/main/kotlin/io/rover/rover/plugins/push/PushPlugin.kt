package io.rover.rover.plugins.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.DrawableRes
import android.support.v4.app.NavUtils
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.TaskStackBuilder
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.data.http.WireEncoderInterface
import io.rover.rover.plugins.userexperience.TopLevelNavigation
import io.rover.rover.plugins.userexperience.experience.containers.StandaloneExperienceHostActivity
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException

open class PushPlugin(
    private val applicationContext: Context,

    // TODO change to private val pushTokenTransmissionChannel: PushTokenTransmissionChannel,
    private val eventsPlugin: EventsPluginInterface,

    private val wireEncoder: WireEncoderInterface,

    private val topLevelNavigation: TopLevelNavigation,

    private val notificationActionRoutingBehaviour: NotificationActionRoutingBehaviourInterface,

    /**
     * A small icon is necessary for Android push notifications.  Pass a resid.
     *
     * Android design guidelines suggest that you use a multi-level drawable for your application
     * icon, such that you can specify one of its levels that is most appropriate as a single-colour
     * silhouette that can be used in the Android notification drawer.
     */
    @param:DrawableRes
    private val smallIconResId: Int,

    /**
     * The drawable level of [smallIconResId] that should be used for the icon silhouette used in
     * the notification drawer.
     */
    private val smallIconDrawableLevel: Int = 0,

    /**
     * This Channel Id will be used for any push notifications arrive without an included Channel
     * Id.
     */
    private val defaultChannelId: String? = null
): PushPluginInterface {

    override fun onTokenRefresh(token: String?) {
        // so, we need the token to be consumable from a FirebasePushTokenContextProvider

        // TODO to make things safer for GCM consumers, which may be calling this off the main
        // thread, manually delegate this to the main thread just in case.
        Handler(Looper.getMainLooper()).post {
            eventsPlugin.setPushToken(token)
        }
    }

    override fun onMessageReceivedData(parameters: Map<String, String>) {
        // if we have been called, then:
        // a) the notification does not have a display message component; OR
        // b) the app is running in foreground.

        if(!parameters.containsKey("message")) {
            log.w("Invalid push notification received: `message` data parameter not present. Possibly was a Display-only push notification. Ignoring.")
        }

        val message = parameters["message"] ?: return
        handleDataMessage(message)
    }

    override fun onMessageReceivedDataAsBundle(parameters: Bundle) {
        val message = parameters.getString("message") ?: return
        handleDataMessage(message)
    }

    /**
     * Generates an intent for displaying main screen of your activity.  Used to insert a back stack
     * entry in new Android Tasks created by the user tapping on a push notification from Rover with
     * Notification Centre is *not* enabled.
     *
     * The default is to use either the parent activity you may have specified in your custom entry
     * for [StandaloneExperienceHostActivity] in your Manifest, or the default Main activity of your
     * app.  However if you are building a single-Activity app or using some other sort of custom
     * routing arrangement (say, Fragments or Conductor), you may want to override this behaviour to
     * build a custom Intent.
     */
    open fun activityMainScreenIntent(): Intent {
        return NavUtils.getParentActivityIntent(applicationContext, StandaloneExperienceHostActivity::class.java)!!
    }

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)

    private fun handleDataMessage(message: String) {
        val (pushNotification, id) = try {
            val messageObject = JSONObject(message)
            val attributes = messageObject.getJSONObject("attributes")
            val id = messageObject.getInt("id")
            Pair(wireEncoder.decodeNotification(attributes), id)
        } catch (e: JSONException) {
            log.w("Invalid push notification received: `$message`, resulting in '${e.message}'. Ignoring.")
            return
        } catch (e: MalformedURLException) {
            log.w("Invalid push notification received: `$message`, resulting in '${e.message}'. Ignoring.")
            return
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(applicationContext, pushNotification.channelId ?: defaultChannelId ?: NotificationChannel.DEFAULT_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(applicationContext)
        }

        builder.setContentTitle(pushNotification.title)
        builder.setContentText(pushNotification.body)
        builder.setSmallIcon(smallIconResId, smallIconDrawableLevel)

        // so, we need to inject a synthesized backstack.

        // https://developer.android.com/training/implementing-navigation/temporal.html#SynthesizeBackStack

        // we can also allow the developer to specify behaviour they want with a meta-data tag added
        // to their Activity entry.  However, I still suspect that adding an opportunity for developer code
        // to create their own Intent is probably ultimately more powerful.

        // also we'll need a way for developers to specify their own version of StandaloneExperienceHostActivity.

        // for that matter they may to build their own intent even for that so they can have their
        // own host of ExperienceView if they're doing something special.

        // There’s some question as to what to do with any existing Task for the app that may
        // already exist. Replace it, add a second Task, or just add to it?  The right answer here
        // may depend on the implementation of the customer’s app, so maybe just have this be a
        // setting.  See "Launch Modes" on
        // https://developer.android.com/guide/components/activities/tasks-and-back-stack.html.
        // yeah, i think this is the case.


        // TODO: I have to move this dispatch-to-Intent behaviour somewhere where the
        // NotificationListViewModel can make use of it.  just the targetIntent part, I think.

        val targetIntent = notificationActionRoutingBehaviour.notificationActionToIntent(pushNotification.action)

        // now to synthesize the backstack.
        val pendingIntent = TaskStackBuilder.create(applicationContext).apply {
            if(pushNotification.isNotificationCenterEnabled) {
                // inject the Notification Centre for the user's app. TODO: allow user to *configure*
                // what their notification centre is, either with a custom URI template method OR
                // just with a meta-property in their Manifest. but by default we can bundle an Activity that hosts NotificationCentreView, I think.

                // for now, we'll just put some sort of
                addNextIntent(topLevelNavigation.displayNotificationCenterIntent())
            } else {
                // Instead of displaying the notification centre, display the parent activity the user set
                addNextIntent(activityMainScreenIntent())
            }

            // so, targetIntent, since it uses extra data to pass arguments, might be a problem:
            // PendingIntents are value objects, but they do not fully encapsulate any extras data,
            // so they may find themselves "merged".  However, perhaps TaskStackBuilder is handling
            // this problem.
            addNextIntent(targetIntent)
        }.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        // TODO: set large icon and possibly a big picture style as needed by Rich Media values. Protocol to be determined.

        notificationManager.notify(id, builder.build().apply { this.flags = this.flags or Notification.FLAG_AUTO_CANCEL })
    }
}
