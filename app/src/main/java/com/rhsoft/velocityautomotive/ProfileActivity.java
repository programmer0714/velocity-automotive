package com.rhsoft.velocityautomotive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private int userId;
    private TextView tvFavCount, tvCitasCount, tvNoCitas, tvNoFavoritos;
    private RecyclerView rvFavoritos;
    private LinearLayout llCitasContainer;

    // Cliente HTTP reutilizable para todas las peticiones
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Cliente HTTP con timeouts de 15 segundos
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        // Obtener datos del usuario desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        String nombre   = prefs.getString("user_name",  "Usuario");
        String email    = prefs.getString("user_email", "");
        String telefono = prefs.getString("user_phone", "");
        userId          = prefs.getInt("user_id", -1);

        // Bind de vistas
        TextView tvName   = findViewById(R.id.tvProfileName);
        TextView tvEmail  = findViewById(R.id.tvProfileEmail);
        TextView tvPhone  = findViewById(R.id.tvProfilePhone);
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        tvFavCount        = findViewById(R.id.tvFavCount);
        tvCitasCount      = findViewById(R.id.tvCitasCount);
        tvNoCitas         = findViewById(R.id.tvNoCitas);
        tvNoFavoritos     = findViewById(R.id.tvNoFavoritos);
        rvFavoritos       = findViewById(R.id.rvFavoritos);

        // Contenedor donde se agregarán las tarjetas de citas
        llCitasContainer = new LinearLayout(this);
        llCitasContainer.setOrientation(LinearLayout.VERTICAL);

        // Insertar el contenedor antes del mensaje "no tienes citas"
        ViewGroup parent = (ViewGroup) tvNoCitas.getParent();
        int index = parent.indexOfChild(tvNoCitas);
        parent.addView(llCitasContainer, index);

        // Asignar datos del usuario a las vistas
        tvName.setText(nombre);
        tvEmail.setText(email);
        tvPhone.setText("📱 " + telefono);
        if (!nombre.isEmpty()) {
            tvAvatar.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }

        // Botón atrás
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Botón cerrar sesión — borra sesión y va al login
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // Configurar RecyclerView de favoritos
        rvFavoritos.setLayoutManager(new LinearLayoutManager(this));
        rvFavoritos.setNestedScrollingEnabled(false);

        // Cargar datos si el usuario está logueado
        if (userId != -1) {
            loadFavorites();
            loadAppointments();
        }
    }

    // ============================================
    // PASO 1: Obtener IDs de favoritos del usuario
    // PASO 2: Por cada car_id, consultar el carro
    // ============================================
    private void loadFavorites() {
        new Thread(() -> {
            try {
                // Traer solo los car_id de la tabla favorites
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
                Log.d("VELOCITY", "Favoritos IDs: " + body);

                JSONArray favArr = new JSONArray(body);
                int count = favArr.length();

                // Actualizar contador en pantalla
                runOnUiThread(() -> tvFavCount.setText(String.valueOf(count)));

                if (count == 0) {
                    // Sin favoritos — mostrar mensaje
                    runOnUiThread(() -> {
                        tvNoFavoritos.setVisibility(View.VISIBLE);
                        rvFavoritos.setVisibility(View.GONE);
                    });
                    return;
                }

                // Construir lista de car_ids para consultar en una sola petición
                // Ej: car_id=in.(1,2,3)
                StringBuilder ids = new StringBuilder();
                for (int i = 0; i < favArr.length(); i++) {
                    if (i > 0) ids.append(",");
                    ids.append(favArr.getJSONObject(i).optInt("car_id"));
                }

                // Consultar todos los carros favoritos de una vez
                String carsUrl = ApiClient.SUPABASE_URL +
                        "/rest/v1/cars?id=in.(" + ids + ")&select=id,brand,model,image_url,price";

                Request carsRequest = new Request.Builder()
                        .url(carsUrl)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();

                Response carsResponse = httpClient.newCall(carsRequest).execute();
                String carsBody = carsResponse.body().string();
                Log.d("VELOCITY", "Carros favoritos: " + carsBody);

                JSONArray carsArr = new JSONArray(carsBody);

                // Mostrar lista de favoritos con imágenes
                runOnUiThread(() -> {
                    tvNoFavoritos.setVisibility(View.GONE);
                    rvFavoritos.setVisibility(View.VISIBLE);
                    rvFavoritos.setAdapter(new FavoritosAdapter(carsArr));
                });

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error favoritos: " + e.getMessage());
            }
        }).start();
    }

    // ============================================
    // PASO 1: Obtener citas del usuario
    // PASO 2: Por cada cita, consultar el carro
    // ============================================
    private void loadAppointments() {
        new Thread(() -> {
            try {
                // Traer todas las citas del usuario ordenadas por fecha
                String url = ApiClient.SUPABASE_URL +
                        "/rest/v1/appointments?user_id=eq." + userId +
                        "&select=id,car_id,tipo,fecha,hora,estado,nota&order=fecha.asc";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();

                Response response = httpClient.newCall(request).execute();
                String body = response.body().string();
                Log.d("VELOCITY", "Citas: " + body);

                JSONArray citasArr = new JSONArray(body);
                int count = citasArr.length();

                // Actualizar contador en pantalla
                runOnUiThread(() -> tvCitasCount.setText(String.valueOf(count)));

                if (count == 0) {
                    // Sin citas — mostrar mensaje
                    runOnUiThread(() -> tvNoCitas.setVisibility(View.VISIBLE));
                    return;
                }

                // Para cada cita, consultar los datos del carro
                JSONArray citasConCarro = new JSONArray();
                for (int i = 0; i < citasArr.length(); i++) {
                    JSONObject cita = citasArr.getJSONObject(i);
                    int carId = cita.optInt("car_id", 0);

                    // Consultar info del carro de esta cita
                    String carUrl = ApiClient.SUPABASE_URL +
                            "/rest/v1/cars?id=eq." + carId + "&select=brand,model,image_url";

                    Request carRequest = new Request.Builder()
                            .url(carUrl)
                            .get()
                            .header("apikey", ApiClient.SUPABASE_KEY)
                            .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                            .header("Accept", "application/json")
                            .build();

                    Response carResponse = httpClient.newCall(carRequest).execute();
                    String carBody = carResponse.body().string();
                    JSONArray carArr = new JSONArray(carBody);

                    // Combinar datos de cita + datos del carro
                    if (carArr.length() > 0) {
                        JSONObject carData = carArr.getJSONObject(0);
                        cita.put("car_brand",     carData.optString("brand"));
                        cita.put("car_model",     carData.optString("model"));
                        cita.put("car_image_url", carData.optString("image_url"));
                    }
                    citasConCarro.put(cita);
                }

                // Mostrar tarjetas de citas en pantalla
                JSONArray finalCitas = citasConCarro;
                runOnUiThread(() -> {
                    tvNoCitas.setVisibility(View.GONE);
                    renderCitas(finalCitas);
                });

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error citas: " + e.getMessage());
            }
        }).start();
    }

    // ============================================
    // Infla una tarjeta por cada cita y la agrega
    // al contenedor dinámico llCitasContainer
    // ============================================
    private void renderCitas(JSONArray arr) {
        llCitasContainer.removeAllViews();

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject cita = arr.getJSONObject(i);

                // Datos del carro de la cita
                String brand    = cita.optString("car_brand", "");
                String model    = cita.optString("car_model", "");
                String imageUrl = cita.optString("car_image_url", "");

                // Datos de la cita
                String fecha  = cita.optString("fecha",  "");
                String hora   = cita.optString("hora",   "").substring(0, Math.min(5, cita.optString("hora","").length()));
                String tipo   = cita.optString("tipo",   "");
                String estado = cita.optString("estado", "Pendiente");

                // Inflar layout de tarjeta de cita
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_cita, llCitasContainer, false);

                ImageView ivCita     = itemView.findViewById(R.id.ivCitaCarro);
                TextView tvCarroCita = itemView.findViewById(R.id.tvCitaCarro);
                TextView tvFechaCita = itemView.findViewById(R.id.tvCitaFecha);
                TextView tvTipoCita  = itemView.findViewById(R.id.tvCitaTipo);
                TextView tvEstado    = itemView.findViewById(R.id.tvCitaEstado);

                // Cargar imagen del carro con Glide
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.color.bg_card)
                        .centerCrop()
                        .into(ivCita);

                tvCarroCita.setText(brand + " " + model);
                tvFechaCita.setText("📅 " + fecha + "  🕐 " + hora);
                tvTipoCita.setText(tipo);
                tvEstado.setText(estado);

                // Color según estado de la cita
                if (estado.equalsIgnoreCase("Pendiente")) {
                    tvEstado.setTextColor(getColor(R.color.gold_primary));
                } else if (estado.equalsIgnoreCase("Confirmada")) {
                    tvEstado.setTextColor(0xFF2ECC71);
                } else {
                    tvEstado.setTextColor(0xFFE74C3C);
                }

                llCitasContainer.addView(itemView);

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error render cita: " + e.getMessage());
            }
        }
    }

    // ============================================
    // Adapter para mostrar carros favoritos
    // en un RecyclerView con imagen y precio
    // ============================================
    private class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.FavVH> {

        private final JSONArray data;

        FavoritosAdapter(JSONArray data) { this.data = data; }

        @Override
        public FavVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_favorito, parent, false);
            return new FavVH(v);
        }

        @Override
        public void onBindViewHolder(FavVH holder, int position) {
            try {
                JSONObject car  = data.getJSONObject(position);
                String brand    = car.optString("brand", "");
                String model    = car.optString("model", "");
                String imageUrl = car.optString("image_url", "");
                String price    = car.optString("price", "0");

                // Cargar imagen del carro favorito
                Glide.with(holder.ivCarro.getContext())
                        .load(imageUrl)
                        .placeholder(R.color.bg_card)
                        .centerCrop()
                        .into(holder.ivCarro);

                holder.tvMarca.setText(brand + " " + model);
                holder.tvPrecio.setText("$" + price);

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error bind fav: " + e.getMessage());
            }
        }

        @Override
        public int getItemCount() { return data.length(); }

        class FavVH extends RecyclerView.ViewHolder {
            ImageView ivCarro;
            TextView tvMarca, tvPrecio;
            FavVH(View v) {
                super(v);
                ivCarro  = v.findViewById(R.id.ivFavCarro);
                tvMarca  = v.findViewById(R.id.tvFavMarca);
                tvPrecio = v.findViewById(R.id.tvFavPrecio);
            }
        }
    }
}