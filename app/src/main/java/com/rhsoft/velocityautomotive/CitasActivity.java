package com.rhsoft.velocityautomotive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CitasActivity extends AppCompatActivity {

    private LinearLayout llCitasContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private int userId;

    // Cliente HTTP reutilizable para todas las peticiones
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.citas);

        // Inicializar cliente HTTP con timeouts de 15 segundos
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        // Obtener ID del usuario logueado desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        // Bind de vistas principales
        llCitasContainer = findViewById(R.id.llCitasContainer);
        progressBar      = findViewById(R.id.progressBar);
        tvEmpty          = findViewById(R.id.tvEmpty);

        // Botón atrás — cierra la pantalla
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Cargar citas si el usuario está logueado
        if (userId != -1) {
            loadCitas();
        } else {
            // Usuario no logueado — mostrar mensaje
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Debes iniciar sesión");
        }
    }

    // ============================================
    // PASO 1: Traer todas las citas del usuario
    // PASO 2: Por cada cita, consultar el carro
    // PASO 3: Mostrar tarjeta con imagen + info
    // ============================================
    private void loadCitas() {
        // Mostrar loading y ocultar lista
        showLoading(true);

        new Thread(() -> {
            try {
                // Consultar citas del usuario ordenadas por fecha ascendente
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

                if (citasArr.length() == 0) {
                    // Sin citas — mostrar mensaje vacío
                    runOnUiThread(() -> {
                        showLoading(false);
                        tvEmpty.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                // Para cada cita, consultar los datos del carro relacionado
                JSONArray citasConCarro = new JSONArray();
                for (int i = 0; i < citasArr.length(); i++) {
                    JSONObject cita = citasArr.getJSONObject(i);
                    int carId = cita.optInt("car_id", 0);

                    // Consultar marca, modelo e imagen del carro de esta cita
                    String carUrl = ApiClient.SUPABASE_URL +
                            "/rest/v1/cars?id=eq." + carId +
                            "&select=brand,model,image_url";

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

                    // Combinar datos de la cita + datos del carro en un solo objeto
                    if (carArr.length() > 0) {
                        JSONObject carData = carArr.getJSONObject(0);
                        cita.put("car_brand",     carData.optString("brand"));
                        cita.put("car_model",     carData.optString("model"));
                        cita.put("car_image_url", carData.optString("image_url"));
                    }
                    citasConCarro.put(cita);
                }

                // Mostrar todas las tarjetas de citas en pantalla
                JSONArray finalCitas = citasConCarro;
                runOnUiThread(() -> {
                    showLoading(false);
                    renderCitas(finalCitas);
                });

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error citas: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error al cargar citas");
                });
            }
        }).start();
    }

    // ============================================
    // Infla una tarjeta por cada cita y la agrega
    // al contenedor — muestra imagen, nombre del
    // carro, fecha, hora, tipo y estado de la cita
    // ============================================
    private void renderCitas(JSONArray arr) {
        // Limpiar tarjetas anteriores antes de agregar nuevas
        llCitasContainer.removeAllViews();

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject cita = arr.getJSONObject(i);

                // Extraer datos del carro
                String brand    = cita.optString("car_brand", "");
                String model    = cita.optString("car_model", "");
                String imageUrl = cita.optString("car_image_url", "");

                // Extraer datos de la cita
                String fecha  = cita.optString("fecha", "");
                String horaFull = cita.optString("hora", "");
                // Mostrar solo HH:MM sin segundos
                String hora   = horaFull.length() >= 5 ? horaFull.substring(0, 5) : horaFull;
                String tipo   = cita.optString("tipo",   "");
                String nota   = cita.optString("nota",   "");
                String estado = cita.optString("estado", "Pendiente");

                // Inflar el layout de tarjeta de cita
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.itemcitadetail, llCitasContainer, false);

                // Bind de vistas de la tarjeta
                ImageView ivCarro    = itemView.findViewById(R.id.ivCitaCarro);
                TextView tvCarro     = itemView.findViewById(R.id.tvCitaCarro);
                TextView tvFecha     = itemView.findViewById(R.id.tvCitaFecha);
                TextView tvHora      = itemView.findViewById(R.id.tvCitaHora);
                TextView tvTipo      = itemView.findViewById(R.id.tvCitaTipo);
                TextView tvNota      = itemView.findViewById(R.id.tvCitaNota);
                TextView tvEstado    = itemView.findViewById(R.id.tvCitaEstado);

                // Cargar imagen del carro con Glide
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.color.bg_card)
                        .centerCrop()
                        .into(ivCarro);

                // Asignar textos
                tvCarro.setText(brand + " " + model);
                tvFecha.setText("📅 " + fecha);
                tvHora.setText("🕐 " + hora);
                tvTipo.setText("📋 " + tipo);

                // Mostrar nota solo si tiene contenido
                if (!nota.isEmpty()) {
                    tvNota.setVisibility(View.VISIBLE);
                    tvNota.setText("📝 " + nota);
                } else {
                    tvNota.setVisibility(View.GONE);
                }

                tvEstado.setText(estado);

                // Color del badge según estado de la cita
                if (estado.equalsIgnoreCase("Pendiente")) {
                    // Dorado para pendiente
                    tvEstado.setTextColor(getColor(R.color.gold_primary));
                    tvEstado.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0x22C9A84C));
                } else if (estado.equalsIgnoreCase("Confirmada")) {
                    // Verde para confirmada
                    tvEstado.setTextColor(0xFF2ECC71);
                    tvEstado.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0x222ECC71));
                } else {
                    // Rojo para cancelada
                    tvEstado.setTextColor(0xFFE74C3C);
                    tvEstado.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0x22E74C3C));
                }

                // Al tocar la tarjeta → ir al detalle del carro
                int carId = cita.optInt("car_id", 0);
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, CarDetailActivity.class);
                    intent.putExtra("id",       carId);
                    intent.putExtra("brand",    brand);
                    intent.putExtra("model",    model);
                    intent.putExtra("imageUrl", imageUrl);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });

                llCitasContainer.addView(itemView);

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error render cita: " + e.getMessage());
            }
        }
    }

    // Muestra u oculta el loading spinner
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        llCitasContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // Recargar citas al volver a la pantalla
    @Override
    protected void onResume() {
        super.onResume();
        if (userId != -1) loadCitas();
    }
}