
package com.kth.baasio;

import static com.kth.common.utils.LogUtils.LOGE;
import static com.kth.common.utils.LogUtils.makeLogTag;
import static java.net.URLEncoder.encode;
import static org.usergrid.java.client.utils.JsonUtils.parse;
import static org.usergrid.java.client.utils.ObjectUtils.isEmpty;
import static org.usergrid.java.client.utils.UrlUtils.path;

import com.kth.baasio.callback.ApiResponseProgressCallback;
import com.kth.baasio.callback.ClientProgressAsyncTask;
import com.kth.baasio.callback.FileEntityWidthProgress;
import com.kth.baasio.callback.ProgressInfo;
import com.kth.baasio.callback.ProgressListener;
import com.kth.baasio.preferences.BaasPreferences;
import com.kth.baasio.ssl.HttpUtils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.usergrid.android.client.Client;
import org.usergrid.android.client.callbacks.ApiResponseCallback;
import org.usergrid.android.client.callbacks.ClientAsyncTask;
import org.usergrid.java.client.entities.Entity;
import org.usergrid.java.client.entities.User;
import org.usergrid.java.client.response.ApiResponse;
import org.usergrid.java.client.utils.JsonUtils;

import android.content.Context;
import android.os.Build;
import android.webkit.MimeTypeMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class Baasio extends Client {
    private static final String TAG = makeLogTag(Baasio.class);

    private static Baasio mSingleton;

    public static final int MIN_BUFFER_SIZE = 1024 * 4;

    private static int mUploadBuffSize = MIN_BUFFER_SIZE;

    private static int mDownloadBuffSize = MIN_BUFFER_SIZE;

    private String[] mSenderIds;

    private Baasio() {
        super();

        HttpURLConnection.setFollowRedirects(true);
        HttpsURLConnection.setFollowRedirects(true);

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new SourceHttpMessageConverter());
        messageConverters.add(new XmlAwareFormHttpMessageConverter());
        messageConverters.add(new MappingJacksonHttpMessageConverter());

        restTemplate.setMessageConverters(messageConverters);

        if (Build.VERSION.SDK_INT < 9) {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils
                    .getNewHttpClient()));
        }
    }

    /**
     * Get singleton instance
     * 
     * @return instance
     */
    public static Baasio getInstance() {
        if (mSingleton == null) {
            mSingleton = new Baasio();
        }

        return mSingleton;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Clone is not allowed.");
    }

    /**
     * Get upload buffer size(byte)
     * 
     * @return upload buffer size(byte)
     */
    public static int getUploadBuffSize() {
        return mUploadBuffSize;
    }

    /**
     * Set upload buffer size(byte)
     * 
     * @param uploadBuffSize upload buffer size
     */
    public static boolean setUploadBuffSize(int uploadBuffSize) {
        if (uploadBuffSize > MIN_BUFFER_SIZE) {
            Baasio.mUploadBuffSize = uploadBuffSize;
            return true;
        }

        return false;
    }

    /**
     * Get download buffer size(byte)
     * 
     * @return download buffer size(byte)
     */
    public static int getDownloadBuffSize() {
        return mDownloadBuffSize;
    }

    /**
     * Set download buffer size(byte)
     * 
     * @param downloadBuffSize download buffer size
     */
    public static boolean setDownloadBuffSize(int downloadBuffSize) {
        if (downloadBuffSize > MIN_BUFFER_SIZE) {
            Baasio.mDownloadBuffSize = downloadBuffSize;
            return true;
        }

        return false;
    }

    /**
     * Get GCM status return true is enabled, false is disabled.
     */
    public boolean isGcmEnabled() {
        if (mSenderIds != null && mSenderIds.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get GCM sender id return GCM sender id.
     */
    public String[] getGcmSenderIds() {
        return mSenderIds;
    }

    /**
     * Initialize baas client with GCM sender id. This will make GCM enabled.
     * 
     * @param context context
     * @param url API URL address
     * @param organizationId organization id
     * @param applicationId application id
     * @param sender GCM sender id
     */
    public void init(Context context, String url, String organizationId, String applicationId,
            String... sender) {
        init(context, url, organizationId, applicationId);

        mSenderIds = sender;
    }

    protected void assertValidApiUrl() {
        if (isEmpty(getApiUrl())) {
            throw new IllegalArgumentException("No api url specified");
        }
    }

    protected void assertValidOrganizationId() {
        if (isEmpty(getOrganizationId())) {
            throw new IllegalArgumentException("No organization id specified");
        }
    }

    /**
     * Initialize baas client.
     * 
     * @param context context
     * @param url API url address
     * @param organizationId organization id
     * @param applicationId application id
     */
    public void init(Context context, String url, String organizationId, String applicationId) {
        setApiUrl(url);
        setOrganizationId(organizationId);
        setApplicationId(applicationId);

        assertValidApiUrl();
        assertValidOrganizationId();
        assertValidApplicationId();

        setLoggable(BuildConfig.DEBUG);

        String accessToken = BaasPreferences.getAccessToken(context);
        if (!isEmpty(accessToken)) {
            setAccessToken(accessToken);
        }

        String userInfo = BaasPreferences.getUserString(context);
        if (!isEmpty(userInfo)) {
            User user = JsonUtils.parse(userInfo, User.class);

            setLoggedInUser(user);
        }
    }

    /**
     * Delete a user on the server. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param username Username to delete
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse deleteUser(String username) {
        if (isEmpty(username)) {
            throw new IllegalArgumentException("Missing username");
        }

        ApiResponse response = apiRequest(HttpMethod.DELETE, null, null, getOrganizationId(),
                getApplicationId(), "users", username);

        return response;
    }

    /**
     * Delete a user on the server. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param username
     * @param callback
     */
    public void deleteUserAsync(final String username, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return deleteUser(username);
            }
        }).execute();
    }

    /**
     * Update a entity on the server. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param entity
     * @param callback
     */
    public void updateEntityAsync(final Entity entity, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return updateEntity(entity);
            }
        }).execute();
    }

    /**
     * Perform a query request.
     * 
     * @param segments
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse queryEntitiesRequest(String... segments) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(getOrganizationId());
        list.add(getApplicationId());
        list.addAll(Arrays.asList(segments));

        String[] newSegments = list.toArray(new String[list.size()]);

        ApiResponse response = apiRequest(HttpMethod.GET, null, null, newSegments);
        return response;
    }

    /**
     * Perform a query request. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param callback
     * @param segments
     */
    public void queryEntitiesRequestAsync(final ApiResponseCallback callback,
            final String... segments) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return queryEntitiesRequest(segments);
            }
        }).execute();
    }

    /**
     * Update a entity on the server from a set of properties. Properties must
     * include a "type" property. Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param properties
     * @param callback
     */
    public void updateEntityAsync(final Map<String, Object> properties,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return updateEntity(properties);
            }
        }).execute();
    }

    /**
     * Update a entity on the server.
     * 
     * @param entity
     * @return an ApiResponse with the updated entity in it.
     */
    public ApiResponse updateEntity(Entity entity) {
        assertValidApplicationId();
        if (isEmpty(entity.getType())) {
            throw new IllegalArgumentException("Missing entity type");
        }

        if (isEmpty(entity.getUuid().toString())) {
            throw new IllegalArgumentException("Missing entity Uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.PUT, null, entity, getOrganizationId(),
                getApplicationId(), entity.getType(), entity.getUuid().toString());
        return response;
    }

    /**
     * Update a new entity on the server from a set of properties. Properties
     * must include a "type" property.
     * 
     * @param properties
     * @return an ApiResponse with the updated entity in it.
     */
    public ApiResponse updateEntity(Map<String, Object> properties) {
        assertValidApplicationId();
        if (isEmpty(properties.get("type"))) {
            throw new IllegalArgumentException("Missing entity type");
        }

        if (isEmpty(properties.get("uuid"))) {
            throw new IllegalArgumentException("Missing entity uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.PUT, null, properties, getOrganizationId(),
                getApplicationId(), properties.get("type").toString(), properties.get("uuid")
                        .toString());
        return response;
    }

    /**
     * Delete a entity on the server.
     * 
     * @param entity
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse deleteEntity(Entity entity) {
        if (isEmpty(entity.getType())) {
            throw new IllegalArgumentException("Missing entity type");
        }

        if (isEmpty(entity.getUuid().toString())) {
            throw new IllegalArgumentException("Missing entity Uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.DELETE, null, null, getOrganizationId(),
                getApplicationId(), entity.getType(), entity.getUuid().toString());
        return response;
    }

    /**
     * Delete a entity on the server from a set of properties. Properties must
     * include a "type" and "uuid" properties.
     * 
     * @param properties
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse deleteEntity(Map<String, Object> properties) {
        if (isEmpty(properties.get("type"))) {
            throw new IllegalArgumentException("Missing entity type");
        }

        if (isEmpty(properties.get("uuid"))) {
            throw new IllegalArgumentException("Missing entity uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.DELETE, null, null, getOrganizationId(),
                getApplicationId(), properties.get("type").toString(), properties.get("uuid")
                        .toString());
        return response;
    }

    /**
     * Delete a entity on the server.
     * 
     * @param entity
     * @param callback
     */
    public void deleteEntityAsync(final Entity entity, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return deleteEntity(entity);
            }
        }).execute();
    }

    /**
     * Delete a entity on the server from a set of properties. Properties must
     * include a "type" and "uuid" properties. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param properties
     * @param callback
     */
    public void deleteEntityAsync(final Map<String, Object> properties,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return deleteEntity(properties);
            }
        }).execute();
    }

    /**
     * Send PUSH message to devices which appropriate "target" or devices having
     * same "tag" from properties. "target" property:
     * all(default)/tag/device/user "to" property: all -> null, tag -> tag name,
     * device -> device uuid, user -> user uuid "payload": message to be sent.
     * 
     * @param properties
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse sendPush(Map<String, Object> properties) {
        assertValidApplicationId();

        if (isEmpty(properties.get("target"))) {
            properties.put("target", "all");
        }

        ApiResponse response = apiRequest(HttpMethod.POST, null, properties, getOrganizationId(),
                getApplicationId(), "pushes");

        return response;
    }

    /**
     * Send PUSH message to devices which appropriate "target" or devices having
     * same "tag" from properties. Executes asynchronously in background and the
     * callbacks are called in the UI thread. "target" property:
     * all(default)/tag/device/user "to" property: all -> null, tag -> tag name,
     * device -> device uuid, user -> user uuid "payload": message to be sent.
     * 
     * @param properties
     * @param callback
     */
    public void sendPushAsync(final Map<String, Object> properties,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return sendPush(properties);
            }
        }).execute();
    }

    /**
     * Register device on the server for PUSH message. Properties must include a
     * "token"(GCM regId).
     * 
     * @param properties
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse registerDeviceForPush(Map<String, Object> properties) {
        assertValidApplicationId();

        if (isEmpty(properties.get("token"))) {
            throw new IllegalArgumentException("Missing device token");
        }

        if (isEmpty(properties.get("platform"))) {
            properties.put("platform", "G");
        }

        ApiResponse response = apiRequest(HttpMethod.POST, null, properties, getOrganizationId(),
                getApplicationId(), "pushes", "devices");

        return response;
    }

    /**
     * Register device on the server for PUSH message. Properties must include a
     * "token"(GCM regId). Executes asynchronously in background and the
     * callbacks are called in the UI thread.
     * 
     * @param properties
     * @param callback
     */
    public void registerDeviceForPushAsync(final Map<String, Object> properties,
            final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return registerDeviceForPush(properties);
            }
        }).execute();
    }

    /**
     * Update registered device information on the server for PUSH message.
     * Properties must include a "token"(GCM regId).
     * 
     * @param deviceUuid
     * @param properties
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse updateDeviceForPush(String deviceUuid, Map<String, Object> properties) {
        assertValidApplicationId();

        if (isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("Missing device uuid");
        }

        if (isEmpty(properties.get("token"))) {
            throw new IllegalArgumentException("Missing device token");
        }

        if (isEmpty(properties.get("platform"))) {
            properties.put("platform", "G");
        }

        ApiResponse response = apiRequest(HttpMethod.PUT, null, properties, getOrganizationId(),
                getApplicationId(), "pushes", "devices", deviceUuid);

        return response;
    }

    /**
     * Update registered device information on the server for PUSH message.
     * Properties must include a "token"(GCM regId). Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param deviceUuid
     * @param properties
     * @param callback
     */
    public void updateDeviceForPushAsync(final String deviceUuid,
            final Map<String, Object> properties, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return updateDeviceForPush(deviceUuid, properties);
            }
        }).execute();
    }

    /**
     * Get registered device information from the server for PUSH message.
     * 
     * @param deviceUuid
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse getDeviceForPush(String deviceUuid) {
        assertValidApplicationId();

        if (isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("Missing device uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.GET, null, null, getOrganizationId(),
                getApplicationId(), "pushes", "devices", deviceUuid);

        return response;
    }

    /**
     * Get registered device information from the server for PUSH message.
     * Executes asynchronously in background and the callbacks are called in the
     * UI thread.
     * 
     * @param deviceUuid
     * @param callback
     */
    public void getDeviceForPushAsync(final String deviceUuid, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return getDeviceForPush(deviceUuid);
            }
        }).execute();
    }

    /**
     * Delete registered device from the server for PUSH message.
     * 
     * @param deviceUuid
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse deleteDeviceForPush(String deviceUuid) {
        assertValidApplicationId();

        if (isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("Missing device uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.DELETE, null, null, getOrganizationId(),
                getApplicationId(), "pushes", "devices", deviceUuid);

        return response;
    }

    /**
     * Delete registered device from the server for PUSH message. Executes
     * asynchronously in background and the callbacks are called in the UI
     * thread.
     * 
     * @param deviceUuid
     * @param callback
     */
    public void deleteDeviceForPushAsync(final String deviceUuid, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return deleteDeviceForPush(deviceUuid);
            }
        }).execute();
    }

    /**
     * Create a folder on the server.
     * 
     * @param dstPath
     * @param dstFolderName
     */
    public ApiResponse createFolder(String dstPath, String dstFolderName) {
        if (isEmpty(dstPath)) {
            throw new IllegalArgumentException("Missing destination path");
        }

        if (isEmpty(dstFolderName)) {
            throw new IllegalArgumentException("Missing destination folder name");
        }

        if (!dstFolderName.endsWith("/")) {
            dstFolderName = dstFolderName + "/";
        }

        ApiResponse response = apiRequest(HttpMethod.POST, null, null, getOrganizationId(),
                getApplicationId(), "files", dstPath, dstFolderName);

        return response;
    }

    /**
     * Create a folder on the server. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param dstPath
     * @param dstFolderName
     * @param callback
     */
    public void createFolderAsync(final String dstPath, final String dstFolderName,
            final ApiResponseCallback callback) {

        new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return createFolder(dstPath, dstFolderName);
            }
        }.execute();
    }

    private static String getMimeType(String url) {
        String type = null;

        int dotPos = url.lastIndexOf(".") + 1;
        String extension = url.substring(dotPos);

        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    /**
     * Create a file on the server. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param srcFilePath
     * @param dstPath
     * @param dstFileName if null, it will upload with local file's name.
     * @param inline if true, the uploaded file will be displayed automatically
     *            upon browser. if false, will be download as a file.
     * @param callback
     * @return an AsyncTask to cancel upload file.
     */
    public ClientProgressAsyncTask<ApiResponse> createFileAsync(final String srcFilePath,
            final String dstPath, final String dstFileName, final boolean inline,
            final ApiResponseProgressCallback callback) {

        if (isEmpty(srcFilePath)) {
            throw new IllegalArgumentException("Missing source file path");
        }

        File file = new File(srcFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Source file is not exist");
        }

        if (isEmpty(dstPath)) {
            throw new IllegalArgumentException("Missing destination path");
        }

        ClientProgressAsyncTask<ApiResponse> task = new ClientProgressAsyncTask<ApiResponse>(
                callback) {
            private FileEntityWidthProgress entity;

            @Override
            public ApiResponse doTask() {
                final ProgressInfo info = new ProgressInfo();
                final ApiResponse result = new ApiResponse();

                String url;

                File file = new File(srcFilePath);

                String filename = null;
                try {
                    if (isEmpty(dstFileName)) {
                        filename = encode(file.getName(), "UTF-8");
                    } else {
                        filename = encode(dstFileName, "UTF-8");
                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                }

                url = path(getApiUrl(), getOrganizationId(), getApplicationId(), "files", dstPath,
                        filename);

                final long size = file.length();

                entity = new FileEntityWidthProgress(file, null, new ProgressListener() {

                    @Override
                    public void updateTransferred(long transferedBytes) {
                        info.setTotalSize(size);
                        info.setCurrentSize(transferedBytes);
                        publishProgress(info);
                    }
                });

                HttpClient client = HttpUtils.getNewHttpClient();

                LOGE(TAG, "File Create: " + url);

                HttpPost post = new HttpPost(url);
                if (entity != null) {
                    post.setEntity(entity);
                }

                post.setHeader("Authorization", "Bearer " + getAccessToken());

                String mimeType = getMimeType(srcFilePath);

                if (!isEmpty(mimeType)) {
                    post.setHeader("Content-Type", mimeType);
                }

                if (inline) {
                    post.setHeader("Content-Disposition", "inline");
                }

                HttpResponse response = null;
                try {
                    response = client.execute(post);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                }

                StatusLine statusLine = response.getStatusLine();

                if (statusLine != null) {
                    if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
                        if (response.getEntity() != null) {
                            String body = null;
                            try {
                                body = EntityUtils.toString(response.getEntity(), "UTF-8");
                            } catch (ParseException e) {
                                e.printStackTrace();

                                info.setException(e);
                                publishProgress(info);

                                result.setError(e.getMessage());
                                result.setException(e.toString());
                                return result;
                            } catch (IOException e) {
                                e.printStackTrace();

                                info.setException(e);
                                publishProgress(info);

                                result.setError(e.getMessage());
                                result.setException(e.toString());
                                return result;
                            }

                            if (!isEmpty(body))
                                return parse(body, ApiResponse.class);
                        }
                    } else {
                        result.setError("Http Status code is " + statusLine.getStatusCode());
                        return result;
                    }
                }

                result.setError("Unknown Error");
                return result;
            }

            @Override
            public void doCancel() {
                if (entity != null) {
                    entity.cancel();
                }
            }
        };

        task.execute();
        return task;
    }

    /**
     * Update content of a file on the server. Executes asynchronously in
     * background and the callbacks are called in the UI thread.
     * 
     * @param dstFileUuid
     * @param srcFilePath
     * @param inline if true, the uploaded file will be displayed automatically
     *            upon browser. if false, will be download as a file.
     * @param callback
     * @return an AsyncTask to cancel upload file.
     */
    public ClientProgressAsyncTask<ApiResponse> updateFileAsync(final String dstFileUuid,
            final String srcFilePath, final boolean inline,
            final ApiResponseProgressCallback callback) {

        ClientProgressAsyncTask<ApiResponse> task = new ClientProgressAsyncTask<ApiResponse>(
                callback) {
            private FileEntityWidthProgress entity;

            @Override
            public ApiResponse doTask() {
                final ProgressInfo info = new ProgressInfo();
                final ApiResponse result = new ApiResponse();

                File file = new File(srcFilePath);

                String url = path(getApiUrl(), getOrganizationId(), getApplicationId(), "files",
                        dstFileUuid);

                final long size = file.length();

                entity = new FileEntityWidthProgress(file, null, new ProgressListener() {

                    @Override
                    public void updateTransferred(long transferedBytes) {
                        info.setTotalSize(size);
                        info.setCurrentSize(transferedBytes);
                        publishProgress(info);
                    }
                });

                HttpClient client = HttpUtils.getNewHttpClient();

                LOGE(TAG, "File Update: " + url);

                HttpPut put = new HttpPut(url);
                put.setEntity(entity);
                put.setHeader("Authorization", "Bearer " + getAccessToken());

                String mimeType = getMimeType(srcFilePath);

                if (!isEmpty(mimeType)) {
                    put.setHeader("Content-Type", mimeType);
                }

                if (inline) {
                    put.setHeader("Content-Disposition", "inline");
                }

                HttpResponse response = null;
                try {
                    response = client.execute(put);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                }

                StatusLine statusLine = response.getStatusLine();

                if (statusLine != null) {
                    if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
                        if (response.getEntity() != null) {
                            String body = null;
                            try {
                                body = EntityUtils.toString(response.getEntity(), "UTF-8");
                            } catch (ParseException e) {
                                e.printStackTrace();

                                info.setException(e);
                                publishProgress(info);

                                result.setError(e.getMessage());
                                result.setException(e.toString());
                                return result;
                            } catch (IOException e) {
                                e.printStackTrace();

                                info.setException(e);
                                publishProgress(info);

                                result.setError(e.getMessage());
                                result.setException(e.toString());
                                return result;
                            }

                            if (!isEmpty(body))
                                return parse(body, ApiResponse.class);
                        }
                    } else {
                        result.setError("Http Status code is " + statusLine.getStatusCode());
                        return result;
                    }
                }

                result.setError("Unknown Error");
                return result;
            }

            @Override
            public void doCancel() {
                entity.cancel();
            }
        };

        task.execute();
        return task;
    }

    private static String convert(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * Download a file from the server. Executes asynchronously in background
     * and the callbacks are called in the UI thread.
     * 
     * @param srcRemotePath
     * @param dstLocalPath
     * @param dstFileName
     * @param callback
     * @return an AsyncTask to cancel download file.
     */
    public ClientProgressAsyncTask<ApiResponse> getFileAsync(final String srcRemotePath,
            final String dstLocalPath, final String dstFileName,
            final ApiResponseProgressCallback callback) {

        ClientProgressAsyncTask<ApiResponse> task = new ClientProgressAsyncTask<ApiResponse>(
                callback) {
            private boolean isCancelled = false;

            @Override
            public ApiResponse doTask() {
                final ProgressInfo info = new ProgressInfo();
                final ApiResponse result = new ApiResponse();

                String[] pathList = srcRemotePath.split("\\/");

                String fileName = pathList[pathList.length - 1];

                String decodedFileName;
                try {
                    decodedFileName = URLDecoder.decode(fileName, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                }

                String localFilePath;
                if (isEmpty(dstFileName)) {
                    localFilePath = dstLocalPath + File.separator + decodedFileName;
                } else {
                    localFilePath = dstLocalPath + File.separator + dstFileName;
                }

                final File file = new File(localFilePath);

                String[] encodedPathList = new String[pathList.length];
                try {
                    for (int i = 0; i < pathList.length; i++) {
                        encodedPathList[i] = encode(pathList[i], "UTF-8");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                }

                String url = path(getApiUrl(), getOrganizationId(), getApplicationId(), "files",
                        encodedPathList);

                // DefaultHttpClient client = new DefaultHttpClient();
                HttpClient client = HttpUtils.getNewHttpClient();

                LOGE(TAG, "File Get: " + url);

                HttpGet get = new HttpGet(url);

                get.setHeader("Authorization", "Bearer " + getAccessToken());

                HttpResponse response = null;
                try {
                    response = client.execute(get);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();

                    info.setException(e);
                    publishProgress(info);

                    result.setError(e.getMessage());
                    result.setException(e.toString());
                    return result;
                }

                StatusLine statusLine = response.getStatusLine();

                if (statusLine != null) {
                    if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
                        if (response.getEntity() != null) {
                            Header[] clHeaders = response.getHeaders("Content-Length");
                            Header header = clHeaders[0];
                            int totalSize = Integer.parseInt(header.getValue());
                            int downloadedSize = 0;
                            if (response.getEntity() != null) {
                                try {
                                    InputStream stream = response.getEntity().getContent();

                                    byte buf[] = new byte[getDownloadBuffSize()];
                                    int numBytesRead;

                                    String dirPath = file.getAbsolutePath().substring(
                                            0,
                                            file.getAbsolutePath().length()
                                                    - file.getName().length() - 1);
                                    File dir = new File(dirPath);
                                    if (!dir.exists()) {
                                        dir.mkdirs();
                                    } else {
                                        if (!dir.isDirectory()) {
                                            dir.mkdirs();
                                        }
                                    }

                                    file.createNewFile();

                                    BufferedOutputStream fos = new BufferedOutputStream(
                                            new FileOutputStream(file));
                                    do {
                                        numBytesRead = stream.read(buf);
                                        if (numBytesRead > 0) {
                                            fos.write(buf, 0, numBytesRead);
                                            downloadedSize += numBytesRead;

                                            info.setTotalSize(totalSize);
                                            info.setCurrentSize(downloadedSize);

                                            publishProgress(info);
                                        }
                                    } while (numBytesRead > 0 && !isCancelled);
                                    fos.flush();
                                    fos.close();
                                    stream.close();

                                    return result;
                                } catch (IllegalStateException e) {
                                    e.printStackTrace();

                                    info.setException(e);
                                    publishProgress(info);

                                    result.setError(e.getMessage());
                                    result.setException(e.toString());
                                    return result;
                                } catch (IOException e) {
                                    e.printStackTrace();

                                    info.setException(e);
                                    publishProgress(info);

                                    result.setError(e.getMessage());
                                    result.setException(e.toString());
                                    return result;
                                }
                            }
                        }
                    } else {
                        result.setError("Http Status code is " + statusLine.getStatusCode());

                        if (BuildConfig.DEBUG) {
                            try {
                                String resultString = convert(response.getEntity().getContent());
                                LOGE(TAG, resultString);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        return result;
                    }
                }

                result.setError("Unknown Error");
                return result;
            }

            @Override
            public void doCancel() {
                isCancelled = true;
            }
        };

        task.execute();

        return task;
    }

    /**
     * Delete a file from the server.
     * 
     * @param fileUuid
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse deleteFile(String fileUuid) {
        if (isEmpty(fileUuid)) {
            throw new IllegalArgumentException("Missing file uuid");
        }

        ApiResponse response = apiRequest(HttpMethod.DELETE, null, null, getOrganizationId(),
                getApplicationId(), "files", fileUuid);

        return response;
    }

    /**
     * Delete a file from the server. Executes asynchronously in background and
     * the callbacks are called in the UI thread.
     * 
     * @param fileUuid
     * @param callback
     */
    public void deleteFileAsync(final String fileUuid, final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {

            @Override
            public ApiResponse doTask() {
                return deleteFile(fileUuid);
            }
        }).execute();
    }

    /**
     * Get quota information from server. Executes asynchronously in background
     * and the callbacks are called in the UI thread.
     * 
     * @param callback
     */
    public void getQuotaInformationAsync(final ApiResponseCallback callback) {
        (new ClientAsyncTask<ApiResponse>(callback) {
            @Override
            public ApiResponse doTask() {
                return getQuotaInformation();
            }
        }).execute();
    }

    /**
     * Get quota information from server.
     * 
     * @return an ApiResponse with the result in it.
     */
    public ApiResponse getQuotaInformation() {
        assertValidApplicationId();

        ApiResponse response = apiRequest(HttpMethod.GET, null, null, getOrganizationId(),
                getApplicationId(), "files", "information");

        return response;
    }
}
