
package com.kth.baasio.gcm;

import static com.kth.common.utils.LogUtils.LOGD;
import static com.kth.common.utils.LogUtils.makeLogTag;
import static org.usergrid.java.client.utils.ObjectUtils.isEmpty;

import com.google.android.gcm.GCMRegistrar;
import com.kth.baasio.Baasio;
import com.kth.baasio.BuildConfig;
import com.kth.baasio.gcm.callback.GcmTaskCallback;
import com.kth.baasio.preferences.BaasPreferences;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

/**
 * Helper class used to manage GCM with the server.
 */
public final class GcmUtils {
    private static final String TAG = makeLogTag(GcmUtils.class);

    /**
     * Register current device on the server with tags. Executes asynchronously
     * in background and the callbacks are called in the UI thread.
     * 
     * @param context
     * @param tagString
     * @param callback
     */
    public static AsyncTask<Void, Void, String> registerGCMClientWithTags(Context context,
            String tagString, GcmTaskCallback callback) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            if (!isEmpty(callback)) {
                callback.onResponse("Gcm is not enabled!");
            }
            LOGD(TAG, "Gcm is not enabled!");
            return null;
        }

        BaasPreferences.setNeedRegisteredTags(context, tagString);

        return registerGCMClient(context, callback);
    }

    /**
     * Register current device on the server. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param context
     * @param callback
     */
    public static AsyncTask<Void, Void, String> registerGCMClient(Context context,
            GcmTaskCallback callback) {
        GCMRegistrar.checkDevice(context);
        if (BuildConfig.DEBUG) {
            GCMRegistrar.checkManifest(context);
        }

        if (!Baasio.getInstance().isGcmEnabled()) {
            if (!isEmpty(callback)) {
                callback.onResponse("Gcm is not enabled!");
            }

            LOGD(TAG, "Gcm is not enabled!");
            return null;
        }

        final String regId = GCMRegistrar.getRegistrationId(context);

        if (TextUtils.isEmpty(regId)) {
            // Automatically registers application on startup.
            GCMRegistrar.register(context, Baasio.getInstance().getGcmSenderIds());

        } else {
            String currentUsername;
            if (!isEmpty(Baasio.getInstance().getLoggedInUser())) {
                currentUsername = Baasio.getInstance().getLoggedInUser().getUsername();
            } else {
                currentUsername = "";
            }

            // Device is already registered on GCM, check server.
            if (GCMRegistrar.isRegisteredOnServer(context)) {
                String registeredUsername = BaasPreferences.getRegisteredUserName(context);
                if (registeredUsername.equals(currentUsername)) {
                    String tags = BaasPreferences.getRegisteredTags(context);
                    String newTags = BaasPreferences.getNeedRegisteredTags(context);
                    if (tags.equals(newTags)) {
                        if (!isEmpty(callback)) {
                            callback.onResponse("Already registered on the GCM server");
                        }
                        LOGD(TAG, "Already registered on the GCM server");
                    } else {
                        if (!isEmpty(callback)) {
                            callback.onResponse("Already registered on the GCM server. But, need to register again because tags changed.");
                        }
                        LOGD(TAG,
                                "Already registered on the GCM server. But, need to register again because tags changed.");
                        return executeRegisterTask(context, currentUsername, regId, callback);
                    }
                } else {
                    if (!isEmpty(callback)) {
                        callback.onResponse("Already registered on the GCM server. But, need to register again because username changed.");
                    }
                    LOGD(TAG,
                            "Already registered on the GCM server. But, need to register again because username changed.");
                    return executeRegisterTask(context, currentUsername, regId, callback);
                }

            } else {
                // Try to register again, but not on the UI thread.
                // It's also necessary to cancel the task in onDestroy().
                return executeRegisterTask(context, currentUsername, regId, callback);
            }
        }

        return null;
    }

    /**
     * Unregister current device on the server. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param context
     * @param callback
     */
    public static AsyncTask<Void, Void, String> unregisterGCMClient(Context context,
            GcmTaskCallback callback) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            if (!isEmpty(callback)) {
                callback.onResponse("Gcm is not enabled!");
            }
            LOGD(TAG, "Gcm is not enabled!");
            return null;
        }

        return executeUnregisterTask(context, callback);
    }

    private static AsyncTask<Void, Void, String> executeRegisterTask(final Context context,
            final String currentUsername, final String regId, final GcmTaskCallback callback) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            if (!isEmpty(callback)) {
                callback.onResponse("Gcm is not enabled!");
            }
            LOGD(TAG, "Gcm is not enabled!");
            return null;
        }

        return new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                boolean registered = ServerUtilities.register(context, regId, currentUsername);

                String result;
                if (!registered) {
                    // At this point all attempts to register with the app
                    // server failed, so we need to unregister the device
                    // from GCM - the app will try to register again when
                    // it is restarted. Note that GCM will send an
                    // unregistered callback upon completion, but
                    // GCMIntentService.onUnregistered() will ignore it.
                    GCMRegistrar.unregister(context);
                    result = "Register failed.";
                } else {
                    result = "Successfully registered.";
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                if (callback != null) {
                    callback.onResponse(result);
                }
                LOGD(TAG, result);
            }
        }.execute(null, null, null);
    }

    private static AsyncTask<Void, Void, String> executeUnregisterTask(final Context context,
            final GcmTaskCallback callback) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            if (!isEmpty(callback)) {
                callback.onResponse("Gcm is not enabled!");
            }
            LOGD(TAG, "Gcm is not enabled!");
            return null;
        }

        return new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                boolean unregistered = ServerUtilities.unregister(context);

                String result;
                if (unregistered) {
                    result = "Successfully unregistered.";
                } else {
                    result = "Unregister failed.";
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                if (callback != null) {
                    callback.onResponse(result);
                }
                LOGD(TAG, result);
            }
        }.execute(null, null, null);
    }

    /**
     * Register device with regId. This method must be placed in
     * GCMIntentService.onRegistered() Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param context
     * @param regId
     */
    public static boolean register(Context context, String regId) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            LOGD(TAG, "Gcm is not enabled!");
            return false;
        }

        String currentUsername;
        if (!isEmpty(Baasio.getInstance().getLoggedInUser())) {
            currentUsername = Baasio.getInstance().getLoggedInUser().getUsername();
        } else {
            currentUsername = "";
        }

        return ServerUtilities.register(context, regId, currentUsername);
    }

    /**
     * Unregister current device. This method must be placed in
     * GCMIntentService.onUnregistered()
     * 
     * @param context
     */
    public static boolean unregister(final Context context) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            LOGD(TAG, "Gcm is not enabled!");
            return false;
        }

        boolean result = false;
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            result = ServerUtilities.unregister(context);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
            LOGD(TAG, "Ignoring unregister callback");
        }

        return result;
    }

    /**
     * This method must be placed in Application.onDestroy()
     * 
     * @param context
     */
    public static void onDestroy(Context context) {
        if (!Baasio.getInstance().isGcmEnabled()) {
            LOGD(TAG, "Gcm is not enabled!");
            return;
        }

        try {
            GCMRegistrar.onDestroy(context);
        } catch (Exception e) {
            LOGD(TAG, "GCM unregistration error", e);
        }
    }
}
