
package com.kth.baasio.preferences;

import com.kth.common.PlatformSpecificImplementationFactory;
import com.kth.common.preference.SharedPreferenceSaver;

import android.content.Context;
import android.content.SharedPreferences;

public class BaasPreferences {
    private static final String _TAG = "BaasPreferences";

    private static final String DEFAULT_PREFERENCES_NAME = "BaasPreferences";

    private static final String SHARED_PREFERENCE_NAME_USER_STRING = "baas_user_data";

    private static final String SHARED_PREFERENCE_NAME_ACCESS_TOKEN = "baas_access_token";

    private static final String SHARED_PREFERENCE_NAME_REGISTERED_DEVICE_UUID_FOR_PUSH = "baas_registered_device_uuid_for_push";

    private static final String SHARED_PREFERENCE_NAME_REGISTERED_USERNAME_FOR_PUSH = "baas_registered_username_for_push";

    private static final String SHARED_PREFERENCE_NAME_REGISTERED_TAGS_FOR_PUSH = "baas_registered_tag_for_push";

    private static final String SHARED_PREFERENCE_NAME_NEED_REGISTER_TAGS_FOR_PUSH = "baas_need_register_tag_for_push";

    private static SharedPreferences mPreferences;

    private static SharedPreferences getPreference(Context context) {
        if (mPreferences == null)
            mPreferences = context.getSharedPreferences(DEFAULT_PREFERENCES_NAME,
                    Context.MODE_PRIVATE);

        return mPreferences;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Clone is not allowed.");
    }

    public static void clear(Context context) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.clear();

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static void setUserString(Context context, String string) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.putString(SHARED_PREFERENCE_NAME_USER_STRING, string);

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static String getUserString(Context context) {
        SharedPreferences prefs = getPreference(context);
        String result = prefs.getString(SHARED_PREFERENCE_NAME_USER_STRING, "");

        return result;
    }

    public static void setAccessToken(Context context, String string) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.putString(SHARED_PREFERENCE_NAME_ACCESS_TOKEN, string);

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static String getAccessToken(Context context) {
        SharedPreferences prefs = getPreference(context);
        String result = prefs.getString(SHARED_PREFERENCE_NAME_ACCESS_TOKEN, "");

        return result;
    }

    public static void setDeviceUuidForPush(Context context, String string) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.putString(SHARED_PREFERENCE_NAME_REGISTERED_DEVICE_UUID_FOR_PUSH, string);

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static String getDeviceUuidForPush(Context context) {
        SharedPreferences prefs = getPreference(context);
        String result = prefs.getString(SHARED_PREFERENCE_NAME_REGISTERED_DEVICE_UUID_FOR_PUSH, "");

        return result;
    }

    public static void setRegisteredUserName(Context context, String string) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.putString(SHARED_PREFERENCE_NAME_REGISTERED_USERNAME_FOR_PUSH, string);

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static String getRegisteredUserName(Context context) {
        SharedPreferences prefs = getPreference(context);
        String result = prefs.getString(SHARED_PREFERENCE_NAME_REGISTERED_USERNAME_FOR_PUSH, "");

        return result;
    }

    public static void setRegisteredTags(Context context, String string) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.putString(SHARED_PREFERENCE_NAME_REGISTERED_TAGS_FOR_PUSH, string);

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static String getRegisteredTags(Context context) {
        SharedPreferences prefs = getPreference(context);
        String result = prefs.getString(SHARED_PREFERENCE_NAME_REGISTERED_TAGS_FOR_PUSH, "");

        return result;
    }

    public static void setNeedRegisteredTags(Context context, String string) {
        SharedPreferences.Editor editor = getPreference(context).edit();
        editor.putString(SHARED_PREFERENCE_NAME_NEED_REGISTER_TAGS_FOR_PUSH, string);

        SharedPreferenceSaver saver = PlatformSpecificImplementationFactory
                .getSharedPreferenceSaver(context);
        saver.savePreferences(editor, false);
    }

    public static String getNeedRegisteredTags(Context context) {
        SharedPreferences prefs = getPreference(context);
        String result = prefs.getString(SHARED_PREFERENCE_NAME_NEED_REGISTER_TAGS_FOR_PUSH, "");

        return result;
    }
}
