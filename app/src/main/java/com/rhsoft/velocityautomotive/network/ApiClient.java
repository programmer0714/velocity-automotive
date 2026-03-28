package com.rhsoft.velocityautomotive.network;

public class ApiClient {

    public static final String SUPABASE_URL = "https://mbxziwheutxxsptvlifk.supabase.co";
    public static final String SUPABASE_KEY = "sb_publishable_lXWPRGTuWBqJHkEwaCQ6Yw_sUj82Tvk";

    public static String getBaseUrl() {
        return SUPABASE_URL;
    }

    public static String getApiKey() {
        return SUPABASE_KEY;
    }

    public static okhttp3.OkHttpClient getHttpClient() {
        return new okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public static okhttp3.Request.Builder getRequestBuilder(String endpoint) {
        return new okhttp3.Request.Builder()
                .url(SUPABASE_URL + endpoint)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }
}