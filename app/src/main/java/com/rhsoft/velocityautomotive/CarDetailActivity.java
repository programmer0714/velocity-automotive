package com.rhsoft.velocityautomotive;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rhsoft.velocityautomotive.adapter.CarThumbnailAdapter;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarDetailActivity extends AppCompatActivity {

    private ImageView ivCarMain;
    private ObjectAnimator rotateAnimator;
    private boolean isRotating = false;
    private boolean isFavorite = false;
    private Button btnFavorite;
    private int carId;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        carId               = getIntent().getIntExtra("id", 0);
        String brand        = getIntent().getStringExtra("brand");
        String model        = getIntent().getStringExtra("model");
        String location     = getIntent().getStringExtra("location");
        String price        = getIntent().getStringExtra("price");
        String horsepower   = getIntent().getStringExtra("horsepower");
        String acceleration = getIntent().getStringExtra("acceleration");
        String engine       = getIntent().getStringExtra("engine");
        String topSpeed     = getIntent().getStringExtra("topSpeed");
        String imageUrl     = getIntent().getStringExtra("imageUrl");

        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        ivCarMain   = findViewById(R.id.ivCarMain);//imagen
        btnFavorite = findViewById(R.id.btnFavorite);//btn favoritos
        TextView tvBrand    = findViewById(R.id.tvDetailBrand);//marca
        TextView tvModel    = findViewById(R.id.tvDetailModel);
        TextView tvLocation = findViewById(R.id.tvDetailLocation);
        TextView tvPrice    = findViewById(R.id.tvDetailPrice);
        TextView tvTitle    = findViewById(R.id.tvDetailTitle);
        TextView spec1      = findViewById(R.id.spec1Val);
        TextView spec2      = findViewById(R.id.spec2Val);
        TextView spec3      = findViewById(R.id.spec3Val);
        TextView spec4      = findViewById(R.id.spec4Val);

            if (brand != null)        tvBrand.setText(brand);
            if (model != null)      { tvModel.setText(model); tvTitle.setText(model); }
            if (location != null)     tvLocation.setText("📍 " + location);
            if (price != null)        tvPrice.setText("$" + price);
            if (horsepower != null)   spec1.setText(horsepower + " HP");//potencia
            if (acceleration != null) spec2.setText(acceleration + " s");
            if (engine != null)       spec3.setText(engine); //motor
            if (topSpeed != null)     spec4.setText(topSpeed + " km/h");

//descarga imagen de internet para mostrar por pantalla
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.color.bg_card)
                .centerCrop()
                .into(ivCarMain);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
// boton de accion detencta que el usuario aga clic
        findViewById(R.id.btnDetailFav).setOnClickListener(v -> toggleFavorite());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        findViewById(R.id.btnAgendarCita).setOnClickListener(v -> {
            if (userId == -1) {
                Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
                return;
            }
            // abrir la Activity llamada AppointmentActivity
            android.content.Intent intent = new android.content.Intent(this, AppointmentActivity.class);
            intent.putExtra("car_id",    carId);
            intent.putExtra("car_brand", brand);
            intent.putExtra("car_model", model);
            intent.putExtra("imageUrl",  imageUrl);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // verificacion en la bd si el auto ya esta en favoritos

        if (userId != -1) checkIfFavorite();

        // Cargar thumbnails desde Supabase
        loadImagesFromSupabase(imageUrl);

        ivCarMain.setAlpha(0f);
        ivCarMain.animate().alpha(1f).setDuration(600).start();
    }

    // ============================================
    // CARGA IMÁGENES DESDE SUPABASE
    // ============================================
    private void loadImagesFromSupabase(String imageUrl) {
        new Thread(() -> {
            try {
                String url = ApiClient.SUPABASE_URL + "/rest/v1/cars?id=eq." + carId + "&select=images,image_url";
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body().string();
                Log.d("VELOCITY", "Images response: " + body);

                JSONArray arr = new JSONArray(body);
                List<String> thumbs = new ArrayList<>();

                // Siempre agregar imagen principal primero
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    thumbs.add(imageUrl);
                }

                if (arr.length() > 0) {
                    JSONObject car = arr.getJSONObject(0);
                    String images = car.optString("images", "");

                    if (!images.isEmpty()) {
                        // Separar URLs por coma
                        String[] urls = images.split(",");
                        for (String u : urls) {
                            String trimmed = u.trim();
                            if (!trimmed.isEmpty() && !trimmed.equals(imageUrl)) {
                                thumbs.add(trimmed);
                            }
                        }
                    }
                }

                // Si solo hay 1 imagen, duplicarla para que se vea el carrusel
                if (thumbs.size() == 1) {
                    thumbs.add(thumbs.get(0));
                    thumbs.add(thumbs.get(0));
                }

                runOnUiThread(() -> setupThumbnails(thumbs));

            } catch (Exception e) {
                Log.e("VELOCITY", "Error loading images: " + e.getMessage());
                // Fallback — usar imagen principal
                List<String> fallback = new ArrayList<>();
                if (imageUrl != null) {
                    fallback.add(imageUrl);
                    fallback.add(imageUrl);
                    fallback.add(imageUrl);
                }
                runOnUiThread(() -> setupThumbnails(fallback));
            }
        }).start();
    }

    private void setupThumbnails(List<String> thumbs) {
        RecyclerView rvThumbnails = findViewById(R.id.rvThumbnails);
        rvThumbnails.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        CarThumbnailAdapter thumbAdapter = new CarThumbnailAdapter(this, thumbs,
                (url, pos) -> {
                    ivCarMain.animate().alpha(0f).setDuration(150)
                            .withEndAction(() -> {
                                Glide.with(this).load(url).centerCrop().into(ivCarMain);
                                ivCarMain.animate().alpha(1f).setDuration(150).start();
                            }).start();
                });
        rvThumbnails.setAdapter(thumbAdapter);
    }

    // ============================================
    // VERIFICAR SI YA ES FAVORITO
    // ============================================
    private void checkIfFavorite() {
        new Thread(() -> {
            try {
                String url = ApiClient.SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId + "&car_id=eq." + carId;
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                JSONArray arr = new JSONArray(body);
                isFavorite = arr.length() > 0;
                runOnUiThread(() -> updateFavoriteUI());
            } catch (Exception e) {
                Log.e("VELOCITY", "Error check fav: " + e.getMessage());
            }
        }).start();
    }

    // ============================================
    // TOGGLE FAVORITO EN SUPABASE
    // ============================================
    private void toggleFavorite() {
        if (userId == -1) {
            Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavorite) {
            new Thread(() -> {
                try {
                    String url = ApiClient.SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId + "&car_id=eq." + carId;
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
                    Request request = new Request.Builder()
                            .url(url).delete()
                            .header("apikey", ApiClient.SUPABASE_KEY)
                            .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                            .build();
                    client.newCall(request).execute();
                    isFavorite = false;
                    runOnUiThread(() -> {
                        updateFavoriteUI();
                        Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e("VELOCITY", "Error remove fav: " + e.getMessage());
                }
            }).start();
        } else {
            new Thread(() -> {
                try {
                    String url = ApiClient.SUPABASE_URL + "/rest/v1/favorites";
                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("car_id",  carId);
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
                    RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                    Request request = new Request.Builder()
                            .url(url).post(body)
                            .header("apikey", ApiClient.SUPABASE_KEY)
                            .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .build();
                    client.newCall(request).execute();
                    isFavorite = true;
                    runOnUiThread(() -> {
                        updateFavoriteUI();
                        Toast.makeText(this, "❤️ Agregado a favoritos", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e("VELOCITY", "Error add fav: " + e.getMessage());
                }
            }).start();
        }
    }

    private void updateFavoriteUI() {
        if (isFavorite) {
            btnFavorite.setText("❤️  Me gusta");
        } else {
            btnFavorite.setText("♡  Me gusta");
        }
    }

    private void toggle360() {
        if (isRotating) {
            if (rotateAnimator != null) rotateAnimator.cancel();
            ivCarMain.setRotationY(0f);
            isRotating = false;
        } else {
            rotateAnimator = ObjectAnimator.ofFloat(ivCarMain, "rotationY", 0f, 360f);
            rotateAnimator.setDuration(2000);
            rotateAnimator.setInterpolator(new LinearInterpolator());
            rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            rotateAnimator.start();
            isRotating = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rotateAnimator != null) rotateAnimator.cancel();
    }
}