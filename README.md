# Rover Android SDK

SDK 2.0 is under development, and is not yet available.  Please continue with
the [1.x series](https://github.com/RoverPlatform/rover-android/tree/master) for
now.

The in-development README for 2.x follows.

<hr />

## Plugins Overview

1. Data Plugin
2. Location Plugin
3. User Experience Plugin
4. Events Plugin

## Requirements



## Setup and Usage

1. add to build
2. configure your Firebase account for push
3. add firebase to your android app

Google uses their firebase platform for their Android push offering, Firebase
Cloud Messaging.

Follow the directions at [Firebase -> Get Started ->
Android](https://firebase.google.com/docs/android/setup) to add the base
Firebase platform to your app and set up your account tokens.

4. wire up push receivers & auth token

You need to follow all of the standard setup for receiving FCM push messages in
your app.  You need to follow the usual guidance from Google on integrating
Firebase into your Android client app at [Firebase -> Cloud Messaging -> Android
-> Set Up](https://firebase.google.com/docs/cloud-messaging/android/client).
Rover has purposefully does none of these steps for you; the goal is to allow
you to integrate push as you see fit.

One that is done, you will then need to implement a receiver for push messages
(although note this may not be used for all kinds of push notifications issued
from Rover Campaigns).  Follow the guidance at [Firebase -> Cloud Messaging ->
Android -> Receive
Messages](https://firebase.google.com/docs/cloud-messaging/android/receive) to
create your implementation of `FirebaseMessagingService` and add the
`onMessageReceived` template callback method to it.

Once you have your empty `onMessageReceived` method ready to go, this is the
part where you delegate to the Rover SDK to create the notification in the
user's Android notification area by calling `onMessageReceivedData` on the Rover
push plugin.

In Kotlin, it may look something like this:

```kotlin
class MyAppCustomFirebaseReceiver: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val pushPlugin = Rover.sharedInstance.pushPlugin
        pushPlugin.onMessageReceivedData(remoteMessage.data)
    }
}
```

4. wire up location and beacons
5. set up styles & brand colours
6. explore avenues for customization
7. 

## Reference Documentation

## Customization

### Require Login

### Add a Custom app view into an Experience flow

### Dynamically Modify Experiences

For example, if you put custom replacement directives or "variables" of your own
design into text in Experience blocks.

### Use a custom bundled font instead of Roboto

### Forgo the included Activity and Fragment, and use Rover embedded in your single-activity, fragmentless app.

### Other customisations

Consider reading through the below in order to glean the general pattern of
customisation.

Are you customising the Rover SDK in other ways than we've discussed here? We'd
love to hear about it!

### Custom Handling for Push Notifications

## Migrating from SDK 1.x

* changes to general design
  * fdafdsaf
* 

## Further Documentation
