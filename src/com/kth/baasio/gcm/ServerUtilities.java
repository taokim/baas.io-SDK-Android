/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kth.baasio.gcm;

import static org.usergrid.java.client.utils.ObjectUtils.isEmpty;

import com.google.android.gcm.GCMRegistrar;
import com.kth.baasio.Baasio;
import com.kth.baasio.preferences.BaasPreferences;

import org.usergrid.java.client.response.ApiResponse;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Helper class used to communicate with the demo server.
 */
final class ServerUtilities {
    private static final String TAG = "GcmUtils";

    private static final int MAX_ATTEMPTS = 5;

    private static final int BACKOFF_MILLIS = 2000;

    private static final Random sRandom = new Random();

    static List<String> getTagList(String tagString) {
        List<String> result = new ArrayList<String>();

        String[] tags = tagString.split("\\,");
        for (String tag : tags) {
            tag = tag.toLowerCase().trim();
            if (!isEmpty(tag)) {
                result.add(tag);
            }
        }

        return result;
    }

    /**
     * Register this account/device pair within the server.
     * 
     * @param context Current context
     * @param regId The GCM registration ID for this device
     * @return whether the registration succeeded or not.
     */
    public static boolean register(final Context context, final String regId,
            final String currentUsername) {
        Log.i(TAG, "registering device (regId = " + regId + ")");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("token", regId);

        String tagString = BaasPreferences.getNeedRegisteredTags(context);
        List<String> tags = getTagList(tagString);
        properties.put("tags", tags);

        long backoff = BACKOFF_MILLIS + sRandom.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Log.v(TAG, "Attempt #" + i + " to register");
            try {
                post(context, Baasio.getInstance(), true, properties, currentUsername);
                GCMRegistrar.setRegisteredOnServer(context, true);
                BaasPreferences.setRegisteredTags(context, tagString);
                return true;
            } catch (IOException e) {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                Log.e(TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Log.v(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     * 
     * @param context Current context
     * @param regId The GCM registration ID for this device
     */
    public static boolean unregister(final Context context) {
        Log.i(TAG, "unregistering device");

        boolean result = false;

        try {
            post(context, Baasio.getInstance(), false, null, null);
            GCMRegistrar.setRegisteredOnServer(context, false);
            result = true;
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
            Log.d(TAG, "Unable to unregister from application server", e);
        }

        return result;
    }

    /**
     * Issue a POST request to the server.
     * 
     * @param endpoint POST address.
     * @param params request parameters.
     * @throws java.io.IOException propagated from POST.
     */
    private static void post(Context context, Baasio baasio, boolean register,
            Map<String, Object> params, final String currentUsername) throws IOException {
        ApiResponse response = null;

        try {
            if (register) {
                String deviceUuid = BaasPreferences.getDeviceUuidForPush(context);
                if (isEmpty(deviceUuid)) {
                    response = baasio.registerDeviceForPush(params);
                } else {
                    response = baasio.updateDeviceForPush(deviceUuid, params);
                }
            } else {
                String deviceUuid = BaasPreferences.getDeviceUuidForPush(context);
                response = baasio.deleteDeviceForPush(deviceUuid);
            }
        } catch (Exception e) {
            throw new IOException(e.toString());
        }

        if (register) {
            if (response != null) {
                Log.e(TAG, response.toString());

                if (isEmpty(response.getError())) {
                    if (!isEmpty(response.getFirstEntity())) {
                        BaasPreferences.setRegisteredUserName(context, currentUsername);

                        String deviceUuid = response.getFirstEntity().getUuid().toString();
                        BaasPreferences.setDeviceUuidForPush(context, deviceUuid);
                    }
                } else {
                    throw new IOException(response.getErrorDescription());
                }
            }
        } else {
            if (response != null) {
                Log.e(TAG, response.toString());

                if (isEmpty(response.getError())) {
                    BaasPreferences.setRegisteredUserName(context, "");
                    BaasPreferences.setDeviceUuidForPush(context, "");
                } else {
                    throw new IOException(response.getErrorDescription());
                }
            }
        }

    }
}
