
package com.kth.baasio.auth;

import static com.kth.common.utils.LogUtils.makeLogTag;
import static org.usergrid.java.client.utils.ObjectUtils.isEmpty;

import com.kth.baasio.Baasio;
import com.kth.baasio.gcm.GcmUtils;
import com.kth.baasio.gcm.callback.GcmTaskCallback;
import com.kth.baasio.preferences.BaasPreferences;

import org.usergrid.android.client.callbacks.ApiResponseCallback;
import org.usergrid.java.client.response.ApiResponse;

import android.content.Context;
import android.util.Log;

/**
 * Helper class used to authenticate with the server.
 */
public final class AuthUtils {
    private static final String TAG = makeLogTag(AuthUtils.class);

    /**
     * Signup with email. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param context
     * @param username
     * @param name
     * @param email
     * @param password
     * @param callback
     */
    public static void signup(Context context, String username, String name, String email,
            String password, ApiResponseCallback callback) {
        Baasio.getInstance().createUserAsync(username, name, email, password, callback);
    }

    /**
     * Signup with facebook token. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param context
     * @param accessToken
     * @param callback
     */
    public static void signupViaFacebook(final Context context, String accessToken,
            final ApiResponseCallback callback) {
        Baasio.getInstance().authorizeAppUserViaFacebookAsync(accessToken,
                new ApiResponseCallback() {

                    @Override
                    public void onException(Exception e) {
                        callback.onException(e);
                    }

                    @Override
                    public void onResponse(ApiResponse response) {
                        if (response != null) {
                            if (isEmpty(response.getError())) {
                                String token = response.getAccessToken();
                                String user = response.getUser().toString();

                                if (!isEmpty(token)) {
                                    BaasPreferences.setAccessToken(context, token);
                                }

                                if (!isEmpty(user)) {
                                    BaasPreferences.setUserString(context, user);
                                }

                                if (Baasio.getInstance().isGcmEnabled()) {
                                    GcmUtils.registerGCMClient(context, new GcmTaskCallback() {

                                        @Override
                                        public void onResponse(String response) {
                                            Log.i(TAG, response);
                                        }
                                    });
                                }
                            }
                        }

                        callback.onResponse(response);
                    }
                });
    }

    /**
     * Login with email. Executes asynchronously in background and the callbacks
     * are called in the UI thread.
     * 
     * @param context
     * @param email
     * @param password
     * @param callback
     */
    public static void login(final Context context, String email, String password,
            final ApiResponseCallback callback) {
        Baasio.getInstance().authorizeAppUserAsync(email, password, new ApiResponseCallback() {

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }

            @Override
            public void onResponse(ApiResponse response) {
                if (response != null) {
                    if (isEmpty(response.getError())) {
                        String token = response.getAccessToken();
                        String user = response.getUser().toString();

                        if (!isEmpty(token)) {
                            BaasPreferences.setAccessToken(context, token);
                        }

                        if (!isEmpty(user)) {
                            BaasPreferences.setUserString(context, user);
                        }

                        if (Baasio.getInstance().isGcmEnabled()) {
                            GcmUtils.registerGCMClient(context, new GcmTaskCallback() {

                                @Override
                                public void onResponse(String response) {
                                    Log.i(TAG, response);
                                }
                            });
                        }
                    }
                }

                callback.onResponse(response);
            }
        });
    }

    /**
     * Login with facebook token. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param context
     * @param accessToken
     * @param callback
     */
    public static void loginViaFacebook(final Context context, String accessToken,
            final ApiResponseCallback callback) {
        Baasio.getInstance().authorizeAppUserViaFacebookAsync(accessToken,
                new ApiResponseCallback() {

                    @Override
                    public void onException(Exception e) {
                        callback.onException(e);
                    }

                    @Override
                    public void onResponse(ApiResponse response) {
                        if (response != null) {
                            if (isEmpty(response.getError())) {
                                String token = response.getAccessToken();
                                String user = response.getUser().toString();

                                if (!isEmpty(token)) {
                                    BaasPreferences.setAccessToken(context, token);
                                }

                                if (!isEmpty(user)) {
                                    BaasPreferences.setUserString(context, user);
                                }

                                if (Baasio.getInstance().isGcmEnabled()) {
                                    GcmUtils.registerGCMClient(context, new GcmTaskCallback() {

                                        @Override
                                        public void onResponse(String response) {
                                            Log.i(TAG, response);
                                        }
                                    });
                                }
                            }
                        }

                        callback.onResponse(response);
                    }
                });
    }

    /**
     * Logout current user.
     * 
     * @param context
     */
    public static void logout(final Context context) {
        Baasio.getInstance().setLoggedInUser(null);
        Baasio.getInstance().setAccessToken(null);

        BaasPreferences.setAccessToken(context, "");
        BaasPreferences.setUserString(context, "");

        if (Baasio.getInstance().isGcmEnabled()) {
            GcmUtils.registerGCMClient(context, new GcmTaskCallback() {

                @Override
                public void onResponse(String response) {
                    Log.i(TAG, response);
                }
            });
        }
    }

    /**
     * Unsubscribe a user. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param context
     * @param email
     * @param callback
     */
    public static void unsubscribe(final Context context, String email,
            final ApiResponseCallback callback) {
        Baasio.getInstance().deleteUserAsync(email, new ApiResponseCallback() {

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }

            @Override
            public void onResponse(ApiResponse response) {
                if (response != null) {
                    if (isEmpty(response.getError())) {
                        Baasio.getInstance().setLoggedInUser(null);
                        Baasio.getInstance().setAccessToken(null);

                        BaasPreferences.setAccessToken(context, "");
                        BaasPreferences.setUserString(context, "");

                        if (Baasio.getInstance().isGcmEnabled()) {
                            GcmUtils.unregisterGCMClient(context, new GcmTaskCallback() {

                                @Override
                                public void onResponse(String response) {
                                    Log.i(TAG, response);
                                }
                            });
                        }
                    }
                }

                callback.onResponse(response);
            }
        });
    }
}
