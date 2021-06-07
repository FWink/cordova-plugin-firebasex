package org.apache.cordova.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

public abstract class FirebasePluginMessageReceiver {

    public FirebasePluginMessageReceiver() {
        FirebasePluginMessageReceiverManager.register(this);
    }

    /**
     * Concrete subclasses should override this and return true if they handle the received message.
     *
     * @param remoteMessage
     * @return true if the received message was handled by the receiver so should not be handled by FirebasePluginMessagingService.onMessageReceived()
     */
    public abstract boolean onMessageReceived(RemoteMessage remoteMessage);

    /**
     * Same as {@link #onMessageReceived(RemoteMessage)} but adds an Android context parameter.
     * By default this simply calls {@link #onMessageReceived(RemoteMessage)}.
     * Implementing classes should only use one of these overloads.
     * @param remoteMessage
     * @param context
     * @return
     */
    public boolean onMessageReceived(RemoteMessage remoteMessage, Context context) {
        return onMessageReceived(remoteMessage);
    }

    /**
     * Concrete subclasses should override this and return true if they handle the message bundle before it's sent to FirebasePlugin.sendMessage().
     *
     * @param bundle
     * @return true if the received bundle was handled by the receiver so should not be handled by FirebasePlugin.
     */
    public abstract boolean sendMessage(Bundle bundle);

    /**
     * Same as {@link #sendMessage(Bundle)} but adds an Android context parameter.
     * By default this simply calls {@link #sendMessage(Bundle)}.
     * Implementing classes should only use one of these overloads.
     * @param bundle
     * @param context
     * @return
     */
    public boolean sendMessage(Bundle bundle, Context context) {
        return sendMessage(bundle);
    }
}
