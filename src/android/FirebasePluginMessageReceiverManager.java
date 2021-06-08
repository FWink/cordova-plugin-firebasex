package org.apache.cordova.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FirebasePluginMessageReceiverManager {

    private static final String TAG = "FirebasePlugin";

    private static List<FirebasePluginMessageReceiver> receivers = new ArrayList<FirebasePluginMessageReceiver>();

    /**
     * Initializes the receiver manager with the given context.
     * To be called on application start.
     * @param context
     */
    public static void initialize(Context context) {
        tryInitializeStaticReceivers(context);
    }

    public static void register(FirebasePluginMessageReceiver receiver) {
        addReceiver(receiver);
    }

    public static boolean onMessageReceived(RemoteMessage remoteMessage, Context context) {
        boolean handled = false;
        for (FirebasePluginMessageReceiver receiver : getReceivers(context)) {
            boolean wasHandled = receiver.onMessageReceived(remoteMessage, context);
            if (wasHandled) {
                handled = true;
            }
        }

        return handled;
    }

    public static boolean sendMessage(Bundle bundle, Context context) {
            boolean handled = false;
            for (FirebasePluginMessageReceiver receiver : getReceivers(context)) {
                boolean wasHandled = receiver.sendMessage(bundle, context);
                if (wasHandled) {
                    handled = true;
                }
            }

            return handled;
        }

    /**
     * Adds the given receiver both to our in-memory list of receivers
     * and to our persistent list of static receiver if applicable
     * ({@link #addStaticReceiver(Context, FirebasePluginMessageReceiver)}).
     * @param receiver
     */
    private static void addReceiver(FirebasePluginMessageReceiver receiver) {

        receivers.add(receiver);

        //register static receiver if applicable
        addStaticReceiver(null, receiver);
    }

    /**
     * Returns a list of all registered receivers.
     * @param context
     * @return
     */
    private static Collection<FirebasePluginMessageReceiver> getReceivers(Context context) {
        tryInitializeStaticReceivers(context);
        return receivers;
    }

    //<editor-fold desc="Initialization and static receivers">

    private static final String PREFERENCES_TAG = "FirebasePlugin";
    private static final String PREFERENCES_KEY_STATIC_RECEIVERS = "FCM.ReceiverManager.Receivers.Static";

    private static boolean staticReceiversInitialized;
    private static boolean staticReceiversInitializing;

    private static SharedPreferences staticReceiversPreferences;

    /**
     * Loads a list of registered static receivers classes ({@link FirebasePluginMessageReceiverStatic})
     * and instantiates one receiver per class.
     */
    private static void initializeStaticReceivers(Context context) {

        Collection<FirebasePluginMessageReceiver> staticReceives = new ArrayList<>();
        Set<Class<? extends FirebasePluginMessageReceiver>> receiverClassesNew = new HashSet<>();

        for (Class<? extends FirebasePluginMessageReceiver> receiverClass : loadStaticReceivers(context)) {

            try {
                FirebasePluginMessageReceiver receiver = receiverClass.getConstructor().newInstance();
                staticReceives.add(receiver);
            }
            catch (InvocationTargetException e) {
                //exception thrown in the constructor. log the exception but keep this class around
                FirebasePlugin.handleExceptionWithoutContext(e);
            }
            catch (Exception e) {
                //some other reflection related error => drop the class
                Log.e(TAG, String.format("Could not instantiate static receiver of type %s. Missing a public zero argument constructor?", receiverClass.getSimpleName()), e);
                continue;
            }

            receiverClassesNew.add(receiverClass);
        }

        //the constructors we're calling here cause those instances to be added to our in-memory list of receivers
        //in theory, the constructors may cause additional receivers to be attached
        //=> we check on the current list of receivers to be extra neat
        for (FirebasePluginMessageReceiver receiver : receivers) {
            if (receiver instanceof FirebasePluginMessageReceiverStatic) {
                receiverClassesNew.add(receiver.getClass());
            }
        }

        //save our new list of static receiver classes
        saveStaticReceivers(context, receiverClassesNew);
    }

    /**
     * Calls {@link #initializeStaticReceivers(Context)} if required.
     * @param context
     */
    private static void tryInitializeStaticReceivers(Context context) {

        if (!staticReceiversInitialized) {
            //double check
            synchronized (FirebasePluginMessageReceiverManager.class) {
                if (!staticReceiversInitialized) {

                    if (getStaticReceiversPreferences(context) == null) {
                        //cannot initialize yet
                        return;
                    }

                    staticReceiversInitializing = true;
                    initializeStaticReceivers(context);
                    staticReceiversInitializing = false;
                    staticReceiversInitialized = true;
                }
            }
        }
    }

    /**
     * Loads a list of registered static receivers classes ({@link FirebasePluginMessageReceiverStatic})
     * from persistent storage.
     * Registered classes that do not exist anymore or that do not match our criteria anymore
     * are dropped here.
     * @param context
     * @return
     */
    private static Set<Class<? extends FirebasePluginMessageReceiver>> loadStaticReceivers(Context context) {

        SharedPreferences preferences = getStaticReceiversPreferences(context);

        Set<String> classNames = preferences.getStringSet(PREFERENCES_KEY_STATIC_RECEIVERS, Collections.emptySet());
        Set<Class<? extends FirebasePluginMessageReceiver>> classes = new HashSet<>();

        for (String className : classNames) {
            try {
                Class<?> receiverClass = Class.forName(className);

                if (!FirebasePluginMessageReceiver.class.isAssignableFrom(receiverClass) ||
                    !FirebasePluginMessageReceiverStatic.class.isAssignableFrom(receiverClass)) {
                    //class must have changed at some point => drop it
                    continue;
                }

                //noinspection unchecked
                classes.add((Class<? extends FirebasePluginMessageReceiver>) receiverClass);
            }
            catch (ClassNotFoundException e) {
                Log.i(TAG, String.format(Locale.US, "Static receiver class does not exist anymore: %s", className), e);
            }
        }

        return classes;
    }

    /**
     * Saves the given static receiver classes ({@link FirebasePluginMessageReceiverStatic})
     * in a persistent storage.
     * @param context
     * @param receivers
     */
    private static void saveStaticReceivers(Context context, Collection<Class<? extends FirebasePluginMessageReceiver>> receivers) {

        SharedPreferences preferences = getStaticReceiversPreferences(context);

        Set<String> classNames = new HashSet<>();
        for (Class<? extends FirebasePluginMessageReceiver> receiver : receivers) {
            classNames.add(receiver.getName());
        }

        preferences.edit()
            .putStringSet(PREFERENCES_KEY_STATIC_RECEIVERS, classNames)
            .apply();
    }

    /**
     * Saves this receiver's class to disk if it is a static receiver
     * ({@link FirebasePluginMessageReceiverStatic}).
     * It can later be retrieved via {@link #loadStaticReceivers(Context)}.
     * @param context
     * @param receiver
     */
    private static void addStaticReceiver(Context context, FirebasePluginMessageReceiver receiver) {
        if (!(receiver instanceof FirebasePluginMessageReceiverStatic))
            return;

        if (!staticReceiversInitialized) {
            if (staticReceiversInitializing) {
                //at this point the receiver has already been added to #receivers
                // and will be saved as last step in #initializeStaticReceivers
                return;
            }

            tryInitializeStaticReceivers(context);
        }

        //load -> add -> save
        synchronized (FirebasePluginMessageReceiverManager.class) {
            if (getStaticReceiversPreferences(context) == null) {
                //cannot save yet
                return;
            }

            Set<Class<? extends FirebasePluginMessageReceiver>> receiverClasses = loadStaticReceivers(context);
            if (receiverClasses.add(receiver.getClass())) {
                //save
                saveStaticReceivers(context, receiverClasses);
            }
        }
    }

    /**
     * Returns the shared preferences where our list of static receiver classes is stored.
     * @param context
     * @return May be null if context is null and if this has never been called before
     * with a non-null context.
     */
    private static SharedPreferences getStaticReceiversPreferences(Context context) {
        if (staticReceiversPreferences != null)
            return staticReceiversPreferences;
        if (context == null)
            return null;
        staticReceiversPreferences = context.getSharedPreferences(PREFERENCES_TAG, Context.MODE_PRIVATE);
        return staticReceiversPreferences;
    }
    //</editor-fold>
}
