package org.apache.cordova.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.app.Notification;
import android.text.TextUtils;
import android.text.Html;
import android.text.Spanned;
import android.content.ContentResolver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Canvas;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";

    static final String defaultSmallIconName = "notification_icon";
    static final String defaultLargeIconName = "notification_icon_large";

    static final String imageTypeCircle = "circle";
    static final String imageTypeBigPicture = "big_picture";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String refreshedToken) {
        try{
            super.onNewToken(refreshedToken);
            Log.d(TAG, "Refreshed token: " + refreshedToken);
            FirebasePlugin.sendToken(refreshedToken);
        }catch (Exception e){
            FirebasePlugin.handleExceptionWithoutContext(e);
        }
    }

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Called when message is received.
     * Called IF message is a data message (i.e. NOT sent from Firebase console)
     * OR if message is a notification message (e.g. sent from Firebase console) AND app is in foreground.
     * Notification messages received while app is in background will not be processed by this method;
     * they are handled internally by the OS.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try{
            // [START_EXCLUDE]
            // There are two types of messages data messages and notification messages. Data messages are handled
            // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
            // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
            // is in the foreground. When the app is in the background an automatically generated notification is displayed.
            // When the user taps on the notification they are returned to the app. Messages containing both notification
            // and data payloads are treated as notification messages. The Firebase console always sends notification
            // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
            // [END_EXCLUDE]

            // Pass the message to the receiver manager so any registered receivers can decide to handle it
            boolean wasHandled = FirebasePluginMessageReceiverManager.onMessageReceived(remoteMessage, this);
            if (wasHandled) {
                Log.d(TAG, "Message was handled by a registered receiver");

                // Don't process the message in this method.
                return;
            }

            if(FirebasePlugin.applicationContext == null){
                FirebasePlugin.applicationContext = this.getApplicationContext();
            }

            // TODO(developer): Handle FCM messages here.
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            String messageType;
            String title = null;
            String titleLocKey = null;
            String[] titleLocArgs = null;
            String body = null;
            String bodyLocKey = null;
            String[] bodyLocArgs = null;
            String bodyHtml = null;
            String tag = null;
            String id = null;
            String sound = null;
            String vibrate = null;
            String light = null;
            String color = null;
            String icon = null;
            String channelId = null;
            String visibility = null;
            String priority = null;
            String image = null;
            String imageType = null;
            boolean foregroundNotification = false;

            Map<String, String> data = remoteMessage.getData();

            if (remoteMessage.getNotification() != null) {
                // Notification message payload
                Log.i(TAG, "Received message: notification");
                messageType = "notification";
                id = remoteMessage.getMessageId();
                RemoteMessage.Notification notification = remoteMessage.getNotification();
                title = notification.getTitle();
                titleLocKey = notification.getTitleLocalizationKey();
                titleLocArgs = notification.getTitleLocalizationArgs();
                body = notification.getBody();
                bodyLocKey = notification.getBodyLocalizationKey();
                bodyLocArgs = notification.getBodyLocalizationArgs();
                tag = notification.getTag();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channelId = notification.getChannelId();
                }
                sound = notification.getSound();
                color = notification.getColor();
                icon = notification.getIcon();
                if (notification.getImageUrl() != null) {
                    image = notification.getImageUrl().toString();
                }
                if (!TextUtils.isEmpty(titleLocKey)) {
                    int titleId = getResources().getIdentifier(titleLocKey, "string", getPackageName());
                    title = String.format(getResources().getString(titleId), (Object[])titleLocArgs);
                }
                if (!TextUtils.isEmpty(bodyLocKey)) {
                    int bodyId = getResources().getIdentifier(bodyLocKey, "string", getPackageName());
                    body = String.format(getResources().getString(bodyId), (Object[])bodyLocArgs);
                }
            }else{
                Log.i(TAG, "Received message: data");
                messageType = "data";
            }

            if (data != null) {
                // Data message payload
                if(data.containsKey("notification_foreground")){
                    foregroundNotification = true;
                }
                if(data.containsKey("notification_title")) title = data.get("notification_title");
                if(data.containsKey("notification_body")) body = data.get("notification_body");
                if(data.containsKey("notification_tag")) tag = data.get("notification_tag");
                if(data.containsKey("notification_android_body_html")) bodyHtml = data.get("notification_android_body_html");
                if(data.containsKey("notification_android_channel_id")) channelId = data.get("notification_android_channel_id");
                if(data.containsKey("notification_android_id")) id = data.get("notification_android_id");
                if(data.containsKey("notification_android_sound")) sound = data.get("notification_android_sound");
                if(data.containsKey("notification_android_vibrate")) vibrate = data.get("notification_android_vibrate");
                if(data.containsKey("notification_android_light")) light = data.get("notification_android_light"); //String containing hex ARGB color, miliseconds on, miliseconds off, example: '#FFFF00FF,1000,3000'
                if(data.containsKey("notification_android_color")) color = data.get("notification_android_color");
                if(data.containsKey("notification_android_icon")) icon = data.get("notification_android_icon");
                if(data.containsKey("notification_android_visibility")) visibility = data.get("notification_android_visibility");
                if(data.containsKey("notification_android_priority")) priority = data.get("notification_android_priority");
                if(data.containsKey("notification_android_image")) image = data.get("notification_android_image");
                if(data.containsKey("notification_android_image_type")) imageType = data.get("notification_android_image_type");
            }

            if (TextUtils.isEmpty(tag)) {
                // Make sure we don't accidentally pass an empty tag ""
                // Otherwise we might override notifications the user did not mean to override
                tag = null;
            }
            if (!TextUtils.isEmpty(tag)) {
                // Override the unique FCM ID with an ID based on the tag if given
                // => any previous notification with the same tag gets the same ID and will get updated
                id = tag;
            }

            if (TextUtils.isEmpty(id)) {
                Random rand = new Random();
                int n = rand.nextInt(50) + 1;
                id = Integer.toString(n);
            }

            Log.d(TAG, "From: " + remoteMessage.getFrom());
            Log.d(TAG, "Id: " + id);
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Body: " + body);
            Log.d(TAG, "Tag: " + tag);
            Log.d(TAG, "Sound: " + sound);
            Log.d(TAG, "Vibrate: " + vibrate);
            Log.d(TAG, "Light: " + light);
            Log.d(TAG, "Color: " + color);
            Log.d(TAG, "Icon: " + icon);
            Log.d(TAG, "Channel Id: " + channelId);
            Log.d(TAG, "Visibility: " + visibility);
            Log.d(TAG, "Priority: " + priority);
            Log.d(TAG, "Image: " + image);
            Log.d(TAG, "image Type: " + imageType);


            if (!TextUtils.isEmpty(body) || !TextUtils.isEmpty(title) || (data != null && !data.isEmpty())) {
                boolean showNotification = (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback() || foregroundNotification) && (!TextUtils.isEmpty(body) || !TextUtils.isEmpty(title));
                sendMessage(remoteMessage, data, messageType, id, title, body, bodyHtml, showNotification, sound, vibrate, light, color, icon, channelId, priority, visibility, image, imageType, tag);
            }
        }catch (Exception e){
            FirebasePlugin.handleExceptionWithoutContext(e);
        }
    }

    private void sendMessage(RemoteMessage remoteMessage, Map<String, String> data, String messageType, String id, String title, String body, String bodyHtml, boolean showNotification, String sound, String vibrate, String light, String color, String icon, String channelId, String priority, String visibility, String image, String imageType, String tag) {
        Log.d(TAG, "sendMessage(): messageType="+messageType+"; showNotification="+showNotification+"; id="+id+"; title="+title+"; body="+body+"; sound="+sound+"; vibrate="+vibrate+"; light="+light+"; color="+color+"; icon="+icon+"; channel="+channelId+"; data="+data.toString()+"; tag="+tag);
        Bundle bundle = new Bundle();
        for (String key : data.keySet()) {
            bundle.putString(key, data.get(key));
        }
        bundle.putString("messageType", messageType);
        this.putKVInBundle("id", id, bundle);
        this.putKVInBundle("title", title, bundle);
        this.putKVInBundle("body", body, bundle);
        this.putKVInBundle("body_html", bodyHtml, bundle);
        this.putKVInBundle("tag", tag, bundle);
        this.putKVInBundle("sound", sound, bundle);
        this.putKVInBundle("vibrate", vibrate, bundle);
        this.putKVInBundle("light", light, bundle);
        this.putKVInBundle("color", color, bundle);
        this.putKVInBundle("icon", icon, bundle);
        this.putKVInBundle("channel_id", channelId, bundle);
        this.putKVInBundle("priority", priority, bundle);
        this.putKVInBundle("visibility", visibility, bundle);
        this.putKVInBundle("image", image, bundle);
        this.putKVInBundle("image_type", imageType, bundle);
        this.putKVInBundle("show_notification", String.valueOf(showNotification), bundle);
        this.putKVInBundle("from", remoteMessage.getFrom(), bundle);
        this.putKVInBundle("collapse_key", remoteMessage.getCollapseKey(), bundle);
        this.putKVInBundle("sent_time", String.valueOf(remoteMessage.getSentTime()), bundle);
        this.putKVInBundle("ttl", String.valueOf(remoteMessage.getTtl()), bundle);

        if (showNotification) {

            Intent intent;
            PendingIntent pendingIntent;
            final int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;  // Only add on platform levels that support FLAG_MUTABLE

            if(getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intent = new Intent(this, OnNotificationReceiverActivity.class);
                intent.putExtras(bundle);
                pendingIntent = PendingIntent.getActivity(this, id.hashCode(), intent, flag);
            }else{
                intent = new Intent(this, OnNotificationOpenReceiver.class);
                intent.putExtras(bundle);
                pendingIntent = PendingIntent.getBroadcast(this, id.hashCode(), intent, flag);
            }


            // Channel
            if(channelId == null || !FirebasePlugin.channelExists(channelId)){
                channelId = FirebasePlugin.defaultChannelId;
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.d(TAG, "Channel ID: "+channelId);
            }


            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            notificationBuilder
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            if(bodyHtml != null) {
                notificationBuilder
                    .setContentText(fromHtml(body))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(fromHtml(body)));
            }else{
                notificationBuilder
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body));
            }


            // On Android O+ the sound/lights/vibration are determined by the channel ID
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                // Sound
                if (sound == null) {
                    Log.d(TAG, "Sound: none");
                }else if (sound.equals("default")) {
                    notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    Log.d(TAG, "Sound: default");
                }else{
                    Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + sound);
                    Log.d(TAG, "Sound: custom=" + sound+"; path="+soundPath.toString());
                    notificationBuilder.setSound(soundPath);
                }

                // Light
                if (light != null) {
                    try {
                        String[] lightsComponents = color.replaceAll("\\s", "").split(",");
                        if (lightsComponents.length == 3) {
                            int lightArgb = Color.parseColor(lightsComponents[0]);
                            int lightOnMs = Integer.parseInt(lightsComponents[1]);
                            int lightOffMs = Integer.parseInt(lightsComponents[2]);
                            notificationBuilder.setLights(lightArgb, lightOnMs, lightOffMs);
                            Log.d(TAG, "Lights: color="+lightsComponents[0]+"; on(ms)="+lightsComponents[2]+"; off(ms)="+lightsComponents[3]);
                        }

                    } catch (Exception e) {}
                }

                // Vibrate
                if (vibrate != null){
                    try {
                        String[] sVibrations = vibrate.replaceAll("\\s", "").split(",");
                        long[] lVibrations = new long[sVibrations.length];
                        int i=0;
                        for(String sVibration: sVibrations){
                            lVibrations[i] = Integer.parseInt(sVibration.trim());
                            i++;
                        }
                        notificationBuilder.setVibrate(lVibrations);
                        Log.d(TAG, "Vibrate: "+vibrate);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }


            // Icon
            int defaultSmallIconResID = getResources().getIdentifier(defaultSmallIconName, "drawable", getPackageName());
            int customSmallIconResID = 0;
            if(icon != null){
                customSmallIconResID = getResources().getIdentifier(icon, "drawable", getPackageName());
            }

            if (customSmallIconResID != 0) {
                notificationBuilder.setSmallIcon(customSmallIconResID);
                Log.d(TAG, "Small icon: custom="+icon);
            }else if (defaultSmallIconResID != 0) {
                Log.d(TAG, "Small icon: default="+defaultSmallIconName);
                notificationBuilder.setSmallIcon(defaultSmallIconResID);
            } else {
                Log.d(TAG, "Small icon: application");
                notificationBuilder.setSmallIcon(getApplicationInfo().icon);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                int defaultLargeIconResID = getResources().getIdentifier(defaultLargeIconName, "drawable", getPackageName());
                int customLargeIconResID = 0;
                if(icon != null){
                    customLargeIconResID = getResources().getIdentifier(icon+"_large", "drawable", getPackageName());
                }

                int largeIconResID;
                if (customLargeIconResID != 0 || defaultLargeIconResID != 0) {
                    if (customLargeIconResID != 0) {
                        largeIconResID = customLargeIconResID;
                        Log.d(TAG, "Large icon: custom="+icon);
                    }else{
                        Log.d(TAG, "Large icon: default="+defaultLargeIconName);
                        largeIconResID = defaultLargeIconResID;
                    }
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), largeIconResID));
                }
            }

            // Image
            if (image != null) {
                Log.d(TAG, "Large icon: image="+image);
                Bitmap bitmap = getBitmapFromURL(image);
                if(bitmap != null) {
                    if(imageTypeCircle.equalsIgnoreCase(imageType)) {
                        bitmap = getCircleBitmap(bitmap);
                    }
                    else if(imageTypeBigPicture.equalsIgnoreCase(imageType)) {
                        notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon((Bitmap) null));
                    }
                    notificationBuilder.setLargeIcon(bitmap);
                }
            }

            // Color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int defaultColor = getResources().getColor(getResources().getIdentifier("accent", "color", getPackageName()), null);
                if(color != null){
                    notificationBuilder.setColor(Color.parseColor(color));
                    Log.d(TAG, "Color: custom="+color);
                }else{
                    Log.d(TAG, "Color: default");
                    notificationBuilder.setColor(defaultColor);
                }
            }

            // Visibility
            int iVisibility = NotificationCompat.VISIBILITY_PUBLIC;
            if(visibility != null){
                iVisibility = Integer.parseInt(visibility);
            }
            Log.d(TAG, "Visibility: " + iVisibility);
            notificationBuilder.setVisibility(iVisibility);

            // Priority
            int iPriority = NotificationCompat.PRIORITY_MAX;
            if(priority != null){
                iPriority = Integer.parseInt(priority);
            }
            Log.d(TAG, "Priority: " + iPriority);
            notificationBuilder.setPriority(iPriority);

            // Build notification
            Notification notification = notificationBuilder.build();

            // Display notification
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Log.d(TAG, "show notification: "+notification.toString());
            notificationManager.notify(tag, id.hashCode(), notification);
        }
        // Send to plugin
        FirebasePlugin.sendMessage(bundle, this.getApplicationContext());
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {

        if (bitmap == null) {
            return null;
        }

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        float cx = bitmap.getWidth() / 2;
        float cy = bitmap.getHeight() / 2;
        float radius = cx < cy ? cx : cy;
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    private Spanned fromHtml(String source) {
        if (source != null)
            return Html.fromHtml(source);
        else
            return null;
    }

    private void putKVInBundle(String k, String v, Bundle b){
        if(v != null && !b.containsKey(k)){
            b.putString(k, v);
        }
    }
}
