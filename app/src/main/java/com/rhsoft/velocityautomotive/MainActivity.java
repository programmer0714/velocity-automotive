package com.rhsoft.velocityautomotive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rhsoft.velocityautomotive.adapter.CarAdapter;
import com.rhsoft.velocityautomotive.model.Car;
import com.rhsoft.velocityautomotive.network.ApiClient;
import com.rhsoft.velocityautomotive.Vista.CategoryActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCars;  // la lista visual de carros
    private CarAdapter adapter;   //el traductor de datos
    private List<Car> allCars      = new ArrayList<>();  //todo los carros
    private List<Car> filteredCars = new ArrayList<>();  // filtrar carros
    private View progressBar;
    private TextView tvError, tvRetry;

    // Chips
    private TextView chipTodos, chipFerrari, chipLamborghini,
            chipPorsche, chipMcLaren, chipBugatti, chipAstonMartin;
    private TextView[] allChips;
    private String currentBrand = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkSession(); // verifica si el usuario esta logueado
        setupUserName();  //muestra el nombre indica el heder
        setupViews();     // busca las vistas por id
        setupRecyclerView(); // configura la lista de carros
        setupSearch();    // configura lo busqueda
        setupChips();
        setupBottomNav();  //configura los botones inferiores que son 5
        loadCarsFromApi();
    }

    // Permite entrar si está logueado O si es invitado
    private void checkSession() {
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        boolean isLogged = prefs.getBoolean("is_logged", false);
        boolean isGuest  = prefs.getBoolean("is_guest",  false);

        // Si no tiene sesión ni es invitado → ir al login
        if (!isLogged && !isGuest) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    // Muestra "Invitado" si es modo guest
    // te da la bien benida al usuario
    private void setupUserName() {
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);
        String nombre   = isGuest ? "Invitado" : prefs.getString("user_name", "Usuario");

        TextView tvUserName = findViewById(R.id.tvUserName);
        tvUserName.setText(nombre);

        TextView tvAvatar = findViewById(R.id.tvAvatar);
        tvAvatar.setText(isGuest ? "?" : String.valueOf(nombre.charAt(0)).toUpperCase());
    }


    private void setupViews() {
        progressBar = findViewById(R.id.progressBar); //spiner del carro mientras descarga la imagen
        tvError     = findViewById(R.id.tvError);//erro si falla la coneccion a bd
        tvRetry     = findViewById(R.id.tvRetry); // busca btn de reintentar
// if seguro verifica si exixte
        if (tvRetry != null) {
            tvRetry.setOnClickListener(v -> loadCarsFromApi()); // solo si exixte
        }
    }

    private void setupRecyclerView() {
        rvCars = findViewById(R.id.rvCars);
        rvCars.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CarAdapter(this, filteredCars, (car, position) -> {
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

        rvCars.setAdapter(adapter);
    }

    // ============================================
    // CARGA CARROS DESDE SUPABASE
    // ============================================
    private void loadCarsFromApi() {
        showLoading(true);
        showError(false, "");

        new Thread(() -> {
            try {
                String url = ApiClient.SUPABASE_URL + "/rest/v1/cars?select=*&order=id.asc";
                Log.d("VELOCITY", "➡️ Cars URL: " + url);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d("VELOCITY", "⬅️ Cars código: " + response.code());

                JSONArray carsArray = new JSONArray(responseBody);
                List<Car> carsList = new ArrayList<>();

                for (int i = 0; i < carsArray.length(); i++) {
                    JSONObject c = carsArray.getJSONObject(i);
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
                    carsList.add(car);
                }

                runOnUiThread(() -> {
                    showLoading(false);
                    allCars.clear();
                    allCars.addAll(carsList);
                    filterByBrand(currentBrand);
                    Log.d("VELOCITY", "✅ Carros cargados: " + carsList.size());
                });

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error cars: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(true, "📵 " + e.getMessage());
                });
            }
        }).start();
    }

    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvCars != null)
            rvCars.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(boolean show, String message) {
        if (tvError != null) {
            tvError.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) tvError.setText(message);
        }
        if (tvRetry != null)
            tvRetry.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLocal(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterLocal(String query) {
        filteredCars.clear();
        List<Car> base = getBrandFiltered();
        if (query.isEmpty()) {
            filteredCars.addAll(base);
        } else {
            String lower = query.toLowerCase();
            for (Car car : base) {
                if ((car.getBrand() != null && car.getBrand().toLowerCase().contains(lower)) ||
                        (car.getModel() != null && car.getModel().toLowerCase().contains(lower)) ||
                        (car.getEngine() != null && car.getEngine().toLowerCase().contains(lower))) {
                    filteredCars.add(car);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private List<Car> getBrandFiltered() {
        if (currentBrand.equals("Todos")) return allCars;
        List<Car> result = new ArrayList<>();
        for (Car car : allCars) {
            if (car.getBrand() != null &&
                    car.getBrand().equalsIgnoreCase(currentBrand)) {
                result.add(car);
            }
        }
        return result;
    }

    // ============================================
    // CHIPS POR MARCA
    // ============================================
    private void setupChips() {
        chipTodos        = findViewById(R.id.chipTodos);
        chipFerrari      = findViewById(R.id.chipFerrari);
        chipLamborghini  = findViewById(R.id.chipLamborghini);
        chipPorsche      = findViewById(R.id.chipPorsche);
        chipMcLaren      = findViewById(R.id.chipMcLaren);
        chipBugatti      = findViewById(R.id.chipBugatti);
        chipAstonMartin  = findViewById(R.id.chipAstonMartin);

        allChips = new TextView[]{
                chipTodos, chipFerrari, chipLamborghini,
                chipPorsche, chipMcLaren, chipBugatti, chipAstonMartin
        };
        String[] brands = {
                "Todos", "Ferrari", "Lamborghini",
                "Porsche", "McLaren", "Bugatti", "Aston Martin"
        };

        for (int i = 0; i < allChips.length; i++) {
            final String brand = brands[i];
            allChips[i].setOnClickListener(v -> {
                currentBrand = brand;
                updateChipUI(brand);
                filterByBrand(brand);
            });
        }

        // Todos seleccionado por defecto
        updateChipUI("Todos");
    }

    private void updateChipUI(String selectedBrand) {
        String[] brands = {
                "Todos", "Ferrari", "Lamborghini",
                "Porsche", "McLaren", "Bugatti", "Aston Martin"
        };
        for (int i = 0; i < allChips.length; i++) {
            if (brands[i].equals(selectedBrand)) {
                allChips[i].setBackground(getDrawable(R.drawable.bg_button_gold));
                allChips[i].setTextColor(getColor(R.color.bg_dark));
            } else {
                allChips[i].setBackground(getDrawable(R.drawable.bg_chip));
                allChips[i].setTextColor(getColor(R.color.text_secondary));
            }
        }
    }

    private void filterByBrand(String brand) {
        filteredCars.clear();
        if (brand.equals("Todos")) {
            filteredCars.addAll(allCars);
        } else {
            for (Car car : allCars) {
                if (car.getBrand() != null &&
                        car.getBrand().equalsIgnoreCase(brand)) {
                    filteredCars.add(car);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ============================================
    // BOTTOM NAV con iconos PNG
    // ============================================
    private void setupBottomNav() {
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("is_guest", false);

        // Home — siempre disponible
        findViewById(R.id.btnNavHome).setOnClickListener(v -> {
            currentBrand = "Todos";
            updateChipUI("Todos");
            loadCarsFromApi();
            updateNavUI(R.id.btnNavHome);
        });

        findViewById(R.id.btnNavCategory).setOnClickListener(v -> {
            startActivity(new Intent(this, CategoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        findViewById(R.id.btnNavCalendar).setOnClickListener(v -> {
            startActivity(new Intent(this, CitasActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        findViewById(R.id.btnNavFav).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritosActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        findViewById(R.id.btnNavProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void updateNavUI(int selectedId) {
        int[] navIds = {
                R.id.btnNavHome, R.id.btnNavCategory,
                R.id.btnNavCalendar, R.id.btnNavFav, R.id.btnNavProfile
        };
        int[] imgIds = {
                R.id.imgNavHome, R.id.imgNavCategory,
                R.id.imgNavCalendar, R.id.imgNavFav, R.id.imgNavProfile
        };

        for (int i = 0; i < navIds.length; i++) {
            ImageView img = findViewById(imgIds[i]);
            View nav = findViewById(navIds[i]);
            if (nav != null && img != null) {
                if (navIds[i] == selectedId) {
                    img.setColorFilter(getColor(R.color.gold_primary));
                } else {
                    img.setColorFilter(getColor(R.color.text_secondary));
                }
            }
        }
    }
}