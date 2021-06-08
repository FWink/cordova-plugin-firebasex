package org.apache.cordova.firebase;

/**
 * Classes that both sub-class {@link FirebasePluginMessageReceiver} and implement this interface
 * will get constructed automatically when an FCM notification has been received while the app is terminated.
 * In that state the main activity and the WebView do not exist, neither do any cordova plugins.<br/>
 * This interface is basically a promise that the implementing class has a public zero argument constructor
 * and that it will work fine without any existing Cordova context.
 */
public interface FirebasePluginMessageReceiverStatic {
}
