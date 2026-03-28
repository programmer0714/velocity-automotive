package com.rhsoft.velocityautomotive.Data;

import com.rhsoft.velocityautomotive.network.ApiClient;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class DatabaseSingleton {

    // Variable estática — solo existe UNA en toda la app
    private static DatabaseSingleton instance;

    // El cliente HTTP que hace las peticiones
    private final OkHttpClient client;

    private DatabaseSingleton() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static DatabaseSingleton getInstance() {
        if (instance == null) {
            instance = new DatabaseSingleton();
        }
        return instance;
    }
//prepara una peticion con las credenciales de bd
    public Request.Builder getRequestBuilder(String endpoint) {
        return new Request.Builder()
                .url(ApiClient.SUPABASE_URL + endpoint)
                .header("apikey",        ApiClient.SUPABASE_KEY)
                .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                .header("Accept",        "application/json");
    }

    public OkHttpClient getClient() {
        return client;
    }
}