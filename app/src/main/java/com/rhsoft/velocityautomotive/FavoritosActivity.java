package com.rhsoft.velocityautomotive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rhsoft.velocityautomotive.adapter.CarAdapter;
import com.rhsoft.velocityautomotive.model.Car;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FavoritosActivity extends AppCompatActivity {

    private RecyclerView rvFavoritos;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<Car> favCars = new ArrayList<>();
    private CarAdapter adapter;
    private int userId;

    // Cliente HTTP reutilizable
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritos);

        // Cliente HTTP con timeouts
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        // Obtener userId desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        // Bind vistas
        rvFavoritos = findViewById(R.id.rvFavoritos);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);

        // Botón atrás
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Configurar RecyclerView con el mismo adapter del catálogo
        rvFavoritos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarAdapter(this, favCars, (car, position) -> {
            // Al tocar un carro favorito → abrir detalle del carro
            Intent intent = new Intent(this, CarDetailActivity.class);
            intent.putExtra("id",           car.getId());
            intent.putExtra("brand",        car.getBrand());
            intent.putExtra("model",        car.getModel());
            intent.putExtra("location",     car.getLocation());
            intent.putExtra("engine",       car.getEngine());
            intent.putExtra("price",        car.getPrice());
            intent.putExtra("horsepower",   car.getHorsepower());
            intent.putExtra("acceleration", car.getAcceleration());
            intent.putExtra("topSpeed",     car.getTopSpeed());
            intent.putExtra("imageUrl",     car.getImageUrl());
            intent.putExtra("year",         car.getYear());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        rvFavoritos.setAdapter(adapter);

        // Cargar favoritos desde Supabase
        if (userId != -1) {
            loadFavoritos();
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Debes iniciar sesión");
        }
    }

    // ============================================
    // PASO 1: Traer car_ids de favorites
    // PASO 2: Traer info completa de esos carros
    // ============================================
    private void loadFavoritos() {
        showLoading(true);

        new Thread(() -> {
            try {
                // Traer car_ids del usuario
                String url = ApiClient.SUPABASE_URL +
                        "/rest/v1/favorites?user_id=eq." + userId + "&select=car_id";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();

                Response response = httpClient.newCall(request).execute();
                String body = response.body().string();
                Log.d("VELOCITY", "Fav IDs: " + body);

                JSONArray favArr = new JSONArray(body);

                if (favArr.length() == 0) {
                    // Sin favoritos — mostrar mensaje
                    runOnUiThread(() -> {
                        showLoading(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                // Construir string con los IDs: (1,2,3)
                StringBuilder ids = new StringBuilder();
                for (int i = 0; i < favArr.length(); i++) {
                    if (i > 0) ids.append(",");
                    ids.append(favArr.getJSONObject(i).optInt("car_id"));
                }

                // Traer todos los carros favoritos en una sola petición
                String carsUrl = ApiClient.SUPABASE_URL +
                        "/rest/v1/cars?id=in.(" + ids + ")&select=*";

                Request carsRequest = new Request.Builder()
                        .url(carsUrl)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();

                Response carsResponse = httpClient.newCall(carsRequest).execute();
                String carsBody = carsResponse.body().string();
                Log.d("VELOCITY", "Fav Cars: " + carsBody);

                JSONArray carsArr = new JSONArray(carsBody);
                List<Car> lista = new ArrayList<>();

                // Convertir JSON a objetos Car
                for (int i = 0; i < carsArr.length(); i++) {
                    JSONObject c = carsArr.getJSONObject(i);
                    Car car = new Car();
                    car.setId(c.optInt("id"));
                    car.setBrand(c.optString("brand"));
                    car.setModel(c.optString("model"));
                    car.setLocation(c.optString("location"));
                    car.setEngine(c.optString("engine"));
                    car.setPrice(c.optString("price"));
                    car.setHorsepower(String.valueOf(c.optInt("horsepower")));
                    car.setAcceleration(c.optString("acceleration"));
                    car.setTopSpeed(String.valueOf(c.optInt("top_speed")));
                    car.setImageUrl(c.optString("image_url"));
                    car.setCategory(c.optString("category"));
                    car.setYear(c.optInt("year"));
                    lista.add(car);
                }

                // Mostrar lista de carros favoritos
                runOnUiThread(() -> {
                    showLoading(false);
                    favCars.clear();
                    favCars.addAll(lista);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                });

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error favoritos: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error al cargar favoritos");
                });
            }
        }).start();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFavoritos.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // Recargar al volver (por si el usuario quitó un favorito)
    @Override
    protected void onResume() {
        super.onResume();
        if (userId != -1 && !favCars.isEmpty()) {
            loadFavoritos();
        }
    }
}