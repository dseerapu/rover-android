package io.rover.rover.plugins.push

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
import io.rover.rover.plugins.data.graphql.putProp
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.push.domain.PushNotificationAction
import io.rover.rover.plugins.push.domain.RoverPushNotification
import io.rover.rover.plugins.userexperience.experience.containers.StandaloneExperienceHostActivity
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URI
import java.net.URL

open class PushPlugin(
    private val applicationContext: Context,

    // TODO change to private val pushTokenTransmissionChannel: PushTokenTransmissionChannel,
    private val eventsPlugin: EventsPluginInterface,

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
     * Generates an intent for displaying the Notification Centre.  Used to insert a back stack
     * entry in new Android Tasks created by the user tapping on a push notification from Rover with
     * Notification Centre enabled.
     *
     * While for most use cases you should specify a meta property in your Manifest on the Activity
     * entry that will display your Notification Centre (see Rover documentation), however if you
     * are building a single-Activity app or using some other sort of custom routing arrangement
     * (say, Fragments or Conductor), you may want to override this behaviour to build a custom
     * Intent.
     */
    open fun notificationCenterIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("about:blank"))
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
            Pair(RoverPushNotification.Companion.decodeJson(attributes), id)
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
        builder.setContentText(pushNotification.text)
        builder.setSmallIcon(smallIconResId, smallIconDrawableLevel)

        // so, we need to inject a synthesized backstack.

        // .... actually, may not need the trampoline!  Can use build my whole synthesized backstack
        // and use that itself as a pending intent. nice.

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

        val targetIntent = when(pushNotification.action) {
            is PushNotificationAction.Experience ->
                StandaloneExperienceHostActivity.makeIntent(applicationContext, pushNotification.action.experienceId)
            is PushNotificationAction.Website ->
                // Note: Website URIs come from a trusted source, that is, the app's owner
                // commanding a pushNotification through Rover.  Non-web URI schemes are filtered
                // out, as well.
                Intent(Intent.ACTION_VIEW, Uri.parse(pushNotification.action.websiteUrl.toString()))
            is PushNotificationAction.DeepLink ->
                // Like above, but non-web URI schemes are not being filtered.
                Intent(Intent.ACTION_VIEW, Uri.parse(pushNotification.action.deepLinkUrl.toString()))
        }

        val pendingIntent = TaskStackBuilder.create(applicationContext).apply {
            if(pushNotification.isNotificationCenterEnabled) {
                // inject the Notification Centre for the user's app. TODO: allow user to *configure*
                // what their notification centre is, either with a custom URI template method OR
                // just with a meta-property in their Manifest.

                // for now, we'll just put some sort of
                addNextIntent(notificationCenterIntent())
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

        notificationManager.notify(id, builder.build())
    }
}

internal fun RoverPushNotification.encodeJson(): JSONObject {
    return JSONObject().apply {
        listOf(
            RoverPushNotification::title,
            RoverPushNotification::text,
            RoverPushNotification::channelId,
            RoverPushNotification::contentType,
            RoverPushNotification::read
        ).forEach { putProp(this@encodeJson, it) }

        putProp(this@encodeJson, RoverPushNotification::action) {
            it.encodeJson()
        }
    }
}

internal fun PushNotificationAction.Companion.decodeJson(json: JSONObject): PushNotificationAction {
    val contentType = json.getString("content-type")

    return when(contentType) {
        "website" -> PushNotificationAction.Website(
            websiteUrl = URL(json.getString("website-url"))
        )
        "deep-link" -> PushNotificationAction.DeepLink(
            deepLinkUrl = URI(json.getString("deep-link-url"))
        )
        "experience" -> PushNotificationAction.Experience(
            experienceId = json.getString("experience-id")
        )
        else -> throw JSONException("Unsupported Rover notification content-type.")
    }
}

internal fun PushNotificationAction.encodeJson(): JSONObject {
    return JSONObject().apply {
        when(this@encodeJson) {
            is PushNotificationAction.Experience -> {
                putProp(this@encodeJson, PushNotificationAction.Experience::experienceId)
            }
            is PushNotificationAction.DeepLink -> {
                putProp(this@encodeJson, PushNotificationAction.DeepLink::deepLinkUrl)
            }
            is PushNotificationAction.Website -> {
                putProp(this@encodeJson, PushNotificationAction.Website::websiteUrl)
            }
        }
    }
}

internal fun RoverPushNotification.Companion.decodeJson(json: JSONObject): RoverPushNotification {
    return RoverPushNotification(
        title = json.getString("android-title"),
        text = json.getString("notification-text"),
        channelId = json.optString("channel-id"),
        contentType = json.getString("content-type"),
        read = json.getBoolean("read"),
        isNotificationCenterEnabled = json.getBoolean("isNotificationCenterEnabled"),
        action = PushNotificationAction.Companion.decodeJson(json)
    )
}
