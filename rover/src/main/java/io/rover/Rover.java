package io.rover;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
/*
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;
*/
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.rover.model.BeaconConfiguration;
import io.rover.model.BeaconTransitionEvent;
import io.rover.model.Customer;
import io.rover.model.Device;
import io.rover.model.DeviceUpdateEvent;
import io.rover.model.Event;
import io.rover.model.GeofenceTransitionEvent;
import io.rover.model.GimbalPlaceTransitionEvent;
import io.rover.model.LocationUpdateEvent;
import io.rover.model.Place;

/**
 * Created by ata_n on 2016-03-21.
 */
public class Rover implements EventSubmitTask.Callback {

    protected static String VERSION = "1.5.1";
    protected static Rover mSharedInstance = new Rover();

    private static final String TAG = "Rover";

    private Context mApplicationContext;
    private PendingIntent mLocationPendingIntent;
    private PendingIntent mGeofencePendingIntent;
    private PendingIntent mNearbyMessagesPendingIntent;
    private PendingIntent mAppLaunchPendingIntent;
    private ExecutorService mEventExecutorService = Executors.newSingleThreadExecutor();
    protected ArrayList<RoverObserver> mObservers = new ArrayList<>();
    private NotificationProvider mNotificationProvider;
    private boolean mGimbalMode;

    private Rover() {}

    public static void setup(Application application, RoverConfig config) {
        mSharedInstance.mApplicationContext = application.getApplicationContext();
        mSharedInstance.mNotificationProvider = config.mNotificationProvider;
        Router.setApiKey(config.mAppToken);
        Router.setDeviceId(Device.getInstance().getIdentifier(mSharedInstance.mApplicationContext));

        // Gimbal check
        try {
            Class gmblPlaceManagerClass = Class.forName("com.gimbal.android.PlaceManager");
            mSharedInstance.mGimbalMode = true;
        } catch (ClassNotFoundException e) {
            mSharedInstance.mGimbalMode = false;
        }

        Device.getInstance().setGimbalMode(mSharedInstance.mGimbalMode);

        // Set Cache

        try {
            if (HttpResponseCache.getInstalled() == null) {
                File httpCacheDir = new File(mSharedInstance.mApplicationContext.getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            }
        } catch (IOException e) {
            Log.i(TAG, "HTTP response cache installation failed:" + e);
        }
    }

    public static synchronized void identify(Traits traits) {
        Customer customer = getCustomer();
        if (customer != null) {
            if (traits.hasIdentifier())
                customer.setIdentifier(traits.getIdentifier());

            if (traits.hasFirstName())
                customer.setFirstName(traits.getFirstName());

            if (traits.hasLastName())
                customer.setLastName(traits.getLastName());

            if (traits.hasGender())
                customer.setGender(traits.getGender());

            if (traits.hasAge())
                customer.setAge(traits.getAge());

            if (traits.hasEmail())
                customer.setEmail(traits.getEmail());

            if (traits.hasPhoneNumber())
                customer.setPhoneNumber(traits.getPhoneNumber());

            if (traits.hasTags())
                customer.setTags(traits.getTags());

            if (traits.hasCustomTraits())
                customer.setTraits(traits.getCustomTraits());

            customer.save(mSharedInstance.mApplicationContext);
            Event event = new DeviceUpdateEvent(new Date());
            mSharedInstance.sendEvent(event);
        }
    }

    public static void clearCustomer() {
        Customer customer = getCustomer();
        if (customer != null) {
            customer.clear(mSharedInstance.mApplicationContext);
            Event event = new DeviceUpdateEvent(new Date());
            mSharedInstance.sendEvent(event);
        }
    }

    public static void startMonitoring() {

        if (mSharedInstance.mGimbalMode) {
            Log.e(TAG, "Use `PlaceManager.getInstance().startMonitoring();`");
            return;
        }

        if (!GoogleApiConnection.checkPlayServices(mSharedInstance.mApplicationContext)) {
            Log.e(TAG, "Failed to start monitoring");
            return;
        }

        final LocationRequest locationRequest = new LocationRequest()
                .setInterval(900000)
                .setFastestInterval(900000)
                .setSmallestDisplacement(0)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        GoogleApiConnection connection = new GoogleApiConnection(mSharedInstance.mApplicationContext);
        connection.setCallbacks(new GoogleApiConnection.Callbacks() {
            private int mDisconnectionTry = 2;

            @Override
            public int onConnected(final GoogleApiClient client) {
                final GoogleApiConnection.Callbacks clientCallbacks = this;

                // Location Updates
                if (ContextCompat.checkSelfPermission(mSharedInstance.mApplicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Log.i("LocationServices", "Requesting location updates");
                    LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, mSharedInstance.getLocationPendingIntent())
                            .setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    if (status.isSuccess()) {
                                        Log.i("LocationServices", "Successfully registered for updates");
                                    } else {
                                        Log.e("LocationServices", "Could not register for updates. " + status.getStatusMessage());
                                    }

                                    mDisconnectionTry--;
                                    if (mDisconnectionTry == 0) {
                                        client.disconnect();
                                    }
                                }
                            });
                }

                // Nearby Messages
                Nearby.Messages.subscribe(client, mSharedInstance.getNearbyMessagesPendingIntent(), mSharedInstance.getNearbySubscriptionOptions())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.i("Nearby", "Subscribed successfully.");
                                } else {
                                    Log.e("Nearby", "Could not subscribe. " + status.getStatusMessage());
                                    //handleUnsuccessfulNearbyResult(status);
                                }

                                mDisconnectionTry--;
                                if (mDisconnectionTry == 0) {
                                    client.disconnect();
                                }
                            }
                        });

                return GoogleApiConnection.KEEP_ALIVE;
            }
        });
        connection.connect();
    }

    public static void stopMonitoring() {
        if (mSharedInstance.mGimbalMode) {
            Log.e(TAG, "Use `PlaceManager.getInstance().stopMonitoring();`");
            return;
        }

        GoogleApiConnection connection = new GoogleApiConnection(mSharedInstance.mApplicationContext);
        connection.setCallbacks(new GoogleApiConnection.Callbacks() {
            @Override
            public int onConnected(final GoogleApiClient client) {

                LocationServices.GeofencingApi.removeGeofences(client, mSharedInstance.getGeofencePendingIntent())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.i("GeofenceService", "Successfully stopped monitoring for geofences");
                                } else {
                                    Log.w("GeofenceService", "Failed to stop monitoring for geofences");
                                }

                                Nearby.Messages.unsubscribe(client, mSharedInstance.getNearbyMessagesPendingIntent())
                                        .setResultCallback(new ResultCallback<Status>() {
                                            @Override
                                            public void onResult(@NonNull Status status) {
                                                if (status.isSuccess()) {
                                                    Log.i("Nearby", "Unsubscribed successfuly.");
                                                } else {
                                                    Log.w("Nearby", "Could not unsubscribe");
                                                }

                                                LocationServices.FusedLocationApi.removeLocationUpdates(client, mSharedInstance.getLocationPendingIntent())
                                                        .setResultCallback(new ResultCallback<Status>() {
                                                            @Override
                                                            public void onResult(@NonNull Status status) {
                                                                if (status.isSuccess()) {
                                                                    Log.i("LocationServices", "Successfully unregistered for updates");
                                                                } else {
                                                                    Log.e("LocationServices", "Could not unregister for updates. " + status.getStatusMessage());
                                                                }

                                                                client.disconnect();
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });
                return GoogleApiConnection.KEEP_ALIVE;
            }
        });
        connection.connect();
    }

    public static void addObserver(RoverObserver observer) {
        mSharedInstance.mObservers.add(observer);
    }

    public static void deleteObserver(RoverObserver observer) {
        mSharedInstance.mObservers.remove(observer);
    }

    public interface OnInboxReloadListener {
        void onSuccess(List<io.rover.model.Message> messages);
        void onFailure();
    }

    public static void reloadInbox(final OnInboxReloadListener listener) {
        FetchInboxTask task = new FetchInboxTask();
        task.setCallback(new FetchInboxTask.Callback() {
            @Override
            public void onSuccess(List<io.rover.model.Message> messages) {
                if (listener != null) {
                    listener.onSuccess(messages);
                }
            }
        });
        task.execute();
    }

    public static void submitEvent(Event event) {
        mSharedInstance.sendEvent(event);
    }

    private static Customer getCustomer() {
        if (mSharedInstance.mApplicationContext != null) {
            return Customer.getInstance(mSharedInstance.mApplicationContext);
        } else {
            Log.w(TAG, "Attempted to grab customer before initializing Rover");
            return null;
        }
    }

    private PendingIntent getLocationPendingIntent() {
        if (mLocationPendingIntent == null) {
            Intent intent = new Intent(mApplicationContext, LocationUpdateService.class);
            mLocationPendingIntent = PendingIntent.getService(mApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mLocationPendingIntent;
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent == null) {
            Intent intent = new Intent(mApplicationContext, GeofenceTransitionService.class);
            mGeofencePendingIntent = PendingIntent.getService(mApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mGeofencePendingIntent;
    }

    private PendingIntent getNearbyMessagesPendingIntent() {
        if (mNearbyMessagesPendingIntent == null) {
            Intent intent = new Intent(mApplicationContext, NearbyMessageService.class);
            mNearbyMessagesPendingIntent = PendingIntent.getService(mApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mNearbyMessagesPendingIntent;
    }

    private PendingIntent getAppLaunchPendingIntent() {
        // TODO: intent needs to contain message id
        if (mAppLaunchPendingIntent == null) {
            String packageName = mApplicationContext.getPackageName();
            Intent intent = mApplicationContext.getPackageManager().getLaunchIntentForPackage(packageName);
            mAppLaunchPendingIntent = PendingIntent.getActivity(mApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mAppLaunchPendingIntent;
    }

    private SubscribeOptions getNearbySubscriptionOptions() {
        return new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        Log.i("NearbySubscribe", "No longer subscribing.");
                    }
                })
                .build();
    }

    protected void sendEvent(Event event) {
        EventSubmitTask eventTask = new EventSubmitTask(mApplicationContext, event);
        eventTask.setCallback(this);
        // TODO: this may have to be synchronous
        mEventExecutorService.execute(eventTask);
    }

    @Override
    public void onEventRegistered(Event event) {
        if (event instanceof GeofenceTransitionEvent) {
            GeofenceTransitionEvent gtEvent = (GeofenceTransitionEvent)event;
            Place place = gtEvent.getPlace();

            for (RoverObserver observer : mObservers) {
                if (observer instanceof RoverObserver.GeofenceTransitionObserver) {
                    switch (gtEvent.getGeofenceTransition()) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            ((RoverObserver.GeofenceTransitionObserver) observer).onEnterGeofence(place);
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                            ((RoverObserver.GeofenceTransitionObserver) observer).onExitGeofence(place);
                            break;
                    }
                }
            }
        } else if (event instanceof BeaconTransitionEvent) {
            BeaconTransitionEvent btEvent = (BeaconTransitionEvent)event;
            BeaconConfiguration bc = btEvent.getBeaconConfiguration();

            for (RoverObserver observer : mObservers) {
                if (observer instanceof RoverObserver.BeaconTransitionObserver) {
                    switch (btEvent.getTransition()) {
                        case BeaconTransitionEvent.TRANSITION_ENTER:
                            ((RoverObserver.BeaconTransitionObserver) observer).onEnterBeaconRegion(bc);
                            break;
                        case BeaconTransitionEvent.TRANSITION_EXIT:
                            ((RoverObserver.BeaconTransitionObserver) observer).onExitBeaconRegion(bc);
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onReceivedGeofences(final List geofences) {
        GoogleApiConnection connection = new GoogleApiConnection(mApplicationContext);
        connection.setCallbacks(new GoogleApiConnection.Callbacks() {
            @Override
            public int onConnected(final GoogleApiClient client) {
                LocationServices.GeofencingApi.removeGeofences(client, getGeofencePendingIntent())
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    addGeofences(client);
                                } else {
                                    client.disconnect();
                                }
                            }
                        });

                return GoogleApiConnection.KEEP_ALIVE;
            }

            public void addGeofences(final GoogleApiClient client) {
                GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                builder.addGeofences(geofences);

                GeofencingRequest request = builder.build();

                if (ContextCompat.checkSelfPermission(mSharedInstance.mApplicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationServices.GeofencingApi.addGeofences(client, request, getGeofencePendingIntent())
                            .setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                    if (status.isSuccess()) {
                                        // TODO: Clean up

                                        for (RoverObserver observer : mObservers) {
                                            if (observer instanceof RoverObserver.GeofenceRegistrationObserver) {
                                                ((RoverObserver.GeofenceRegistrationObserver) observer).onRegisteredGeofences(geofences);
                                            }
                                        }
                                    }
                                    client.disconnect();
                                }
                            });
                } else {
                    client.disconnect();
                }
            }
        });
        connection.connect();
    }

    static public void simulateGeofenceEnter(String id) {
        Event event = new GeofenceTransitionEvent(id, Geofence.GEOFENCE_TRANSITION_ENTER, new Date());
        mSharedInstance.sendEvent(event);
    }

    static public void simulateGeofenceExit(String id) {
        Event event = new GeofenceTransitionEvent(id, Geofence.GEOFENCE_TRANSITION_EXIT, new Date());
        mSharedInstance.sendEvent(event);
    }

    public static boolean isRoverMessage(RemoteMessage remoteMessage) {
        Map<String,String> data = remoteMessage.getData();
        return (data != null && data.containsKey("_rover"));
    }

    public static io.rover.model.Message getRoverMessageFromRemoteMessage(RemoteMessage remoteMessage) {
        if (isRoverMessage(remoteMessage)) {
            Map<String,String> data = remoteMessage.getData();

            if (data == null) {
                return null;
            }

            String jsonString = data.get("message");
            if (jsonString == null) {
                return null;
            }

            try {
                JSONObject messageJson = new JSONObject(jsonString);
                if (!messageJson.isNull("id") && !messageJson.isNull("attributes")) {
                    String id = messageJson.getString("id");
                    JSONObject attributes = messageJson.getJSONObject("attributes");

                    ObjectMapper objectMapper = new ObjectMapper();
                    io.rover.model.Message message = (io.rover.model.Message) objectMapper.getObject("messages", id, attributes);
                    return message;
                }

            } catch (JSONException e) {
                Log.e(TAG, "Unable to parse json from remote message");
                return null;
            }
        }

        return  null;
    }

    public static PendingIntent getPendingIntentFromRoverMessage(io.rover.model.Message message) {
        
        if (message == null) {
            return null;
        }

        Context context = mSharedInstance.mApplicationContext;

        if (context == null) {
            return null;
        }

        android.app.TaskStackBuilder taskStackBuilder = android.app.TaskStackBuilder.create(mSharedInstance.mApplicationContext);

        switch (message.getAction()) {
            case Website: {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getURI().toString()));
                taskStackBuilder.addNextIntent(intent);
                return taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            case LandingPage: {
                Intent intent = new Intent(context, RemoteScreenActivity.class);
                intent.setData(getUriFromRoverMessage(message));
                taskStackBuilder.addParentStack(RemoteScreenActivity.class);
                taskStackBuilder.addNextIntent(intent);
                return taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            case DeepLink: {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getURI().toString()));
                taskStackBuilder.addNextIntent(intent);
                return taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            case Experience: {
                Intent intent = new Intent(context, ExperienceActivity.class);
                intent.setData(message.getExperienceUri());
                taskStackBuilder.addParentStack(ExperienceActivity.class);
                taskStackBuilder.addNextIntent(intent);
                return taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            default: {
                return mSharedInstance.getAppLaunchPendingIntent();
            }
        }
    }

    public static void handleRemoteMessage(RemoteMessage remoteMessage) {

        if (!isRoverMessage(remoteMessage)) {
            return;
        }

        io.rover.model.Message message = getRoverMessageFromRemoteMessage(remoteMessage);

        if (message == null) {
            Log.w(TAG, "Unable to handle remote message. Message was null");
            return;
        }

        Context context = mSharedInstance.mApplicationContext;

        if (context == null) {
            Log.w(TAG, "Unable to handle remote message. ApplicationContext was null");
            return;
        }

        PendingIntent pendingIntent = null;
        int smallIcon = R.drawable.rover_notification_icon;
        Bitmap largeIcon = null;
        Uri sound = null;

        if (mSharedInstance.mNotificationProvider != null) {
            pendingIntent = mSharedInstance.mNotificationProvider.getNotificationPendingIntent(message);
            smallIcon = mSharedInstance.mNotificationProvider.getSmallIconForNotification(message);
            largeIcon = mSharedInstance.mNotificationProvider.getLargeIconForNotification(message);
            sound = mSharedInstance.mNotificationProvider.getSoundForNotification(message);
        }

        if (pendingIntent == null) {
            pendingIntent = getPendingIntentFromRoverMessage(message);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)
                .setSound(sound)
                .setContentTitle(message.getTitle())
                .setContentText(message.getText())
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(message.getId(), 12345 /* Rover notification id */, builder.build());
    }

    private static Uri getUriFromRoverMessage(io.rover.model.Message message) {
        return new Uri.Builder().scheme("rover")
                .authority("message")
                .appendPath(message.getId()).build();
    }

    /**
     * Services
     */

    static public class LocationUpdateService extends IntentService {

        public LocationUpdateService() { super("LocationUpdateService"); }

        @Override
        protected void onHandleIntent(Intent intent) {
            LocationResult result = LocationResult.extractResult(intent);
            if (result != null) {
                Event event = new LocationUpdateEvent(result.getLastLocation(), new Date());
                mSharedInstance.sendEvent(event);
            }
        }
    }

    static public class GeofenceTransitionService extends IntentService {

        public GeofenceTransitionService() { super("GeofenceTransitionService"); }

        @Override
        protected void onHandleIntent(Intent intent) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

            if (geofencingEvent.hasError()) {
                Log.e("GeofenceService", "GeofencingEventError: " + geofencingEvent.getErrorCode());
                return;
            }

            // Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            Date date = new Date();

            for (Geofence geofence: triggeringGeofences) {
                Event event = new GeofenceTransitionEvent(geofence.getRequestId(), geofenceTransition, date);
                mSharedInstance.sendEvent(event);
            }
        }
    }

    static public class NearbyMessageService extends IntentService {

        public NearbyMessageService() { super("NearbyMessageService"); }

        @Override
        protected void onHandleIntent(Intent intent) {
            Nearby.Messages.handleIntent(intent, new MessageListener() {
                @Override
                public void onFound(Message message) {
                    handleMessage(BeaconTransitionEvent.TRANSITION_ENTER, message);
                }

                @Override
                public void onLost(Message message) {
                    handleMessage(BeaconTransitionEvent.TRANSITION_EXIT, message);
                }

                private void handleMessage(int transition, Message message) {
                    String type = message.getType();
                    if (!type.equals("rover-configuration-id")){
                        return;
                    }

                    String messageString = new String(message.getContent());

                    Log.i("NearbyMessage", "Message namespaced type: " + message.getNamespace() + "/" + message.getType());
                    Event event = new BeaconTransitionEvent(transition, messageString, new Date());
                    mSharedInstance.sendEvent(event);
                }
            });
        }
    }

    static public class RoverFirebaseInstanceIdService extends FirebaseInstanceIdService {
        @Override
        public void onTokenRefresh() {
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.i(TAG, "Refreshed token: " + token);
            Device.getInstance().setGcmToken(token);
            Event event = new DeviceUpdateEvent(new Date());
            mSharedInstance.sendEvent(event);
        }
    }

    static public class RoverFirebaseMessagingService extends FirebaseMessagingService {
        @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
            Rover.handleRemoteMessage(remoteMessage);
        }
    }
}
