## Auxilliary instructions: Rover set up with Legacy GCM

Please refer to the main README for the bulk of the setup instructions.

To use the legacy Google Cloud Messaging service in lieu of the now-recommended
Firebase Cloud Messaging service, complete the procedure below.

You should only integrate Rover with GCM instead of FCM in the event where other
legacy code in your app is currently holding you back from migrating.

### Ensure GCM is Installed

You will need to ensure that you have followed all of the standard setup for
receiving GCM push messages in your app. You will need to follow the usual
guidance from Google on integrating Firebase into your Android client app at
[Google Cloud Messaging -> GCM Clients -> Set Up a Client App on
Android](https://developers.google.com/cloud-messaging/android/client). Rover
has purposefully does none of these steps for you; the goal is to allow you to
integrate push as you see fit.

In particular, as per that documentation, you will need to supply your own
Intent Service to handle incoming Push Tokens from GCM, and also your own
`InstanceIDListenerService` to be notified of any changes to the push token.
They will look something like the following:

```kotlin
class RegistrationIntentService: IntentService() {

    override fun onHandleIntent(intent: Intent) {
        val instanceID = InstanceID.getInstance(this)
        val token = instanceID.getToken(
            /* YOUR GCM SENDER ID HERE */,
            GoogleCloudMessaging.INSTANCE_ID_SCOPE,
            null
        )

        Rover.sharedInstance.pushPlugin.onTokenRefresh(token)
    }
}

class  MyInstanceIDListenerService: InstanceIDListenerService () {
    override fun onTokenRefresh() {
        Rover.sharedInstance.pushPlugin.onTokenRefresh(
            InstanceID.getToken()
        )
    }
}
```

Remember to add them to your Manifest as per the Google documentation!

### Handle incoming GCM messages

Then you'll need a `GcmListenerService` to actually receive the data push
notifications:

```kotlin
class MyGcmListenerService: GcmListenerService() {
    override fun onMessageReceived(from: String, data: Bundle) {
        val pushPlugin = Rover.sharedInstance.pushPlugin
        pushPlugin.onMessageReceivedDataAsBundle(data)
    }
}
```

Naturally, it too must be properly registered in your manifest as per the Google
documentation.



