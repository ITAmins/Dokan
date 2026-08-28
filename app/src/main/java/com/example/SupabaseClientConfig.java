package com.example;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Dedicated Supabase Client Configuration & Data Client module.
 *
 * Connects directly to the existing Supabase project using the .env configuration.
 * Strictly adheres to the pre-existing, hardened schema:
 * 1. public.user_backups: (user_id [UUID], email [TEXT], backup_data [JSONB], updated_at [TIMESTAMPTZ])
 * 2. public.mawa_cloud_records: (user_id [UUID], domain [TEXT], entity_type [TEXT], entity_id [TEXT], data [JSONB], updated_at [TIMESTAMPTZ], deleted_at [TIMESTAMPTZ])
 *
 * No table creation SQL is executed; operations strictly perform queries and mutations on existing tables with RLS.
 */
public class SupabaseClientConfig {
    private static final String TAG = "SupabaseClientConfig";

    // Config defaults (from .env)
    public static final String SUPABASE_URL = "https://pkpcfksbslbileordrqs.supabase.co";
    public static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_Tp8GPJO0_ee3FpITfqes1A_DMn2BZJO";
    public static final String SUPABASE_JWKS_URL = "https://pkpcfksbslbileordrqs.supabase.co/auth/v1/.well-known/jwks.json";

    // Existing Schema Constants
    public static final String TABLE_USER_BACKUPS = "user_backups";
    public static final String TABLE_MAWA_CLOUD_RECORDS = "mawa_cloud_records";

    public static final String COL_USER_ID = "user_id";
    public static final String COL_BACKUP_NAME = "backup_name";
    public static final String COL_DATA = "data";
    public static final String COL_UPDATED_AT = "updated_at";

    public static final String COL_DOMAIN = "domain";
    public static final String COL_ENTITY_TYPE = "entity_type";
    public static final String COL_ENTITY_ID = "entity_id";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static SupabaseClientConfig instance;
    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final SupabaseAuthManager authManager;

    private final String baseUrl;
    private final String apiKey;

    public interface DataCallback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    private SupabaseClientConfig(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder().build();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.authManager = SupabaseAuthManager.getInstance(this.context);

        String configuredUrl = SUPABASE_URL;
        String configuredKey = SUPABASE_PUBLISHABLE_KEY;

        try {
            java.lang.reflect.Field urlField = BuildConfig.class.getField("SUPABASE_URL");
            if (urlField != null) {
                String val = (String) urlField.get(null);
                if (val != null && !val.trim().isEmpty() && !val.contains("YOUR_")) {
                    configuredUrl = val.trim();
                }
            }
        } catch (Exception ignored) {}

        try {
            java.lang.reflect.Field keyField = BuildConfig.class.getField("SUPABASE_PUBLISHABLE_KEY");
            if (keyField != null) {
                String val = (String) keyField.get(null);
                if (val != null && !val.trim().isEmpty() && !val.contains("YOUR_")) {
                    configuredKey = val.trim();
                }
            }
        } catch (Exception ignored) {}

        this.baseUrl = configuredUrl.endsWith("/") ? configuredUrl.substring(0, configuredUrl.length() - 1) : configuredUrl;
        this.apiKey = configuredKey;
    }

    public static synchronized SupabaseClientConfig getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClientConfig(context);
        }
        return instance;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getRestEndpoint(String table) {
        return baseUrl + "/rest/v1/" + table;
    }

    /**
     * Builds request headers including apikey and Bearer authentication token.
     */
    public Request.Builder newAuthenticatedRequestBuilder(String url) {
        String token = authManager.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            token = apiKey;
        }
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json");
    }

    // =========================================================================
    //  public.user_backups operations (user_id, email, backup_data, updated_at)
    // =========================================================================

    /**
     * Fetch the latest backup record for the authenticated user from public.user_backups.
     */
    public void fetchLatestUserBackup(DataCallback<JsonObject> callback) {
        executor.execute(() -> {
            if (!authManager.isAuthenticated()) {
                if (callback != null) callback.onFailure("User is not authenticated");
                return;
            }

            String userId = authManager.getUserId();
            String url = getRestEndpoint(TABLE_USER_BACKUPS)
                    + "?user_id=eq." + userId
                    + "&order=updated_at.desc&limit=1";

            Request request = newAuthenticatedRequestBuilder(url)
                    .addHeader("Accept", "application/json")
                    .get()
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (callback != null) callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            JsonArray arr = JsonParser.parseString(respStr).getAsJsonArray();
                            if (arr != null && arr.size() > 0) {
                                JsonObject row = arr.get(0).getAsJsonObject();
                                JsonObject backupData = null;
                                if (row.has(COL_DATA) && !row.get(COL_DATA).isJsonNull()) {
                                    backupData = row.getAsJsonObject(COL_DATA);
                                } else if (row.has("backup_data") && !row.get("backup_data").isJsonNull()) {
                                    backupData = row.getAsJsonObject("backup_data");
                                }
                                if (callback != null) callback.onSuccess(backupData);
                            } else {
                                if (callback != null) callback.onSuccess(null); // No backup found
                            }
                        } else {
                            if (callback != null) callback.onFailure("HTTP " + response.code() + ": " + respStr);
                        }
                    } catch (Exception e) {
                        if (callback != null) callback.onFailure("Parse error: " + e.getMessage());
                    }
                }
            });
        });
    }

    /**
     * Upsert a full user backup snapshot into public.user_backups.
     */
    public void saveUserBackup(JsonObject backupData, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            if (!authManager.isAuthenticated()) {
                if (callback != null) callback.onFailure("User is not authenticated");
                return;
            }

            String userId = authManager.getUserId();
            String isoTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());

            JsonObject row = new JsonObject();
            row.addProperty(COL_USER_ID, userId);
            row.addProperty(COL_BACKUP_NAME, "MAWA_AUTO_BACKUP_" + System.currentTimeMillis());
            row.add(COL_DATA, backupData);
            row.addProperty(COL_UPDATED_AT, isoTimestamp);

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, row.toString());
            String url = getRestEndpoint(TABLE_USER_BACKUPS);

            Request request = newAuthenticatedRequestBuilder(url)
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (callback != null) callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful() || response.code() == 201 || response.code() == 200 || response.code() == 204) {
                            if (callback != null) callback.onSuccess(true);
                        } else {
                            if (callback != null) callback.onFailure("HTTP " + response.code() + ": " + respStr);
                        }
                    }
                }
            });
        });
    }

    // =========================================================================
    //  public.mawa_cloud_records operations (user_id, domain, entity_type, entity_id, data, updated_at)
    // =========================================================================

    /**
     * Upsert a single granular entity record into public.mawa_cloud_records.
     */
    public void upsertCloudRecord(String domain, String entityType, String entityId, JsonObject data, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            if (!authManager.isAuthenticated()) {
                if (callback != null) callback.onFailure("User is not authenticated");
                return;
            }

            String userId = authManager.getUserId();
            String isoTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());

            JsonObject row = new JsonObject();
            row.addProperty(COL_USER_ID, userId);
            row.addProperty(COL_DOMAIN, domain != null ? domain : "BUSINESS");
            row.addProperty(COL_ENTITY_TYPE, entityType);
            row.addProperty(COL_ENTITY_ID, entityId);
            row.add(COL_DATA, data);
            row.addProperty(COL_UPDATED_AT, isoTimestamp);

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, row.toString());
            // Upsert via PostgREST resolution
            String url = getRestEndpoint(TABLE_MAWA_CLOUD_RECORDS) + "?on_conflict=user_id,entity_type,entity_id";

            Request request = newAuthenticatedRequestBuilder(url)
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (callback != null) callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful() || response.code() == 201 || response.code() == 200 || response.code() == 204) {
                            if (callback != null) callback.onSuccess(true);
                        } else {
                            if (callback != null) callback.onFailure("HTTP " + response.code() + ": " + respStr);
                        }
                    }
                }
            });
        });
    }

    /**
     * Delete a granular record in public.mawa_cloud_records.
     */
    public void deleteCloudRecord(String entityType, String entityId, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            if (!authManager.isAuthenticated()) {
                if (callback != null) callback.onFailure("User is not authenticated");
                return;
            }

            String userId = authManager.getUserId();
            String url = getRestEndpoint(TABLE_MAWA_CLOUD_RECORDS)
                    + "?user_id=eq." + userId
                    + "&entity_type=eq." + entityType
                    + "&entity_id=eq." + entityId;

            Request request = newAuthenticatedRequestBuilder(url)
                    .delete()
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (callback != null) callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful() || response.code() == 200 || response.code() == 204) {
                            if (callback != null) callback.onSuccess(true);
                        } else {
                            if (callback != null) callback.onFailure("HTTP " + response.code() + ": " + respStr);
                        }
                    }
                }
            });
        });
    }
}
