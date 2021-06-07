package org.apache.cordova.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;

public class FirebasePluginMessageReceiverManager {

    private static List<FirebasePluginMessageReceiver> receivers = new ArrayList<FirebasePluginMessageReceiver>();

    public static void register(FirebasePluginMessageReceiver receiver) {
        receivers.add(receiver);
    }

    public static boolean onMessageReceived(RemoteMessage remoteMessage, Context context) {
        boolean handled = false;
        for (FirebasePluginMessageReceiver receiver : receivers) {
            boolean wasHandled = receiver.onMessageReceived(remoteMessage, context);
            if (wasHandled) {
                handled = true;
            }
        }

        return handled;
    }

    public static boolean sendMessage(Bundle bundle, Context context) {
            boolean handled = false;
            for (FirebasePluginMessageReceiver receiver : receivers) {
                boolean wasHandled = receiver.sendMessage(bundle, context);
                if (wasHandled) {
                    handled = true;
                }
            }

            return handled;
        }
}
