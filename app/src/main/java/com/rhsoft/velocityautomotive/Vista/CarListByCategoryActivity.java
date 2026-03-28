package com.rhsoft.velocityautomotive.Vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rhsoft.velocityautomotive.CarDetailActivity;
import com.rhsoft.velocityautomotive.Controlador.CarController;
import com.rhsoft.velocityautomotive.R;
import com.rhsoft.velocityautomotive.adapter.CarAdapter;
import com.rhsoft.velocityautomotive.model.Car;
import java.util.ArrayList;
import java.util.List;

public class CarListByCategoryActivity extends AppCompatActivity {

    private RecyclerView rvCars;
    private ProgressBar progressBar;
    private TextView tvCategoryTitle, tvCarCount;
    private CarController controller;
    private List<Car> cars = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_list_by_category);

        String category = getIntent().getStringExtra("category");
        String icono    = getIntent().getStringExtra("icono");

        rvCars          = findViewById(R.id.rvCars);
        progressBar     = findViewById(R.id.progressBar);
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvCarCount      = findViewById(R.id.tvCarCount);
        controller      = new CarController();

        tvCategoryTitle.setText(icono + " " + category);

        rvCars.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        loadCars(category);
    }

    private void loadCars(String category) {
        progressBar.setVisibility(View.VISIBLE);
        tvCarCount.setText("Cargando vehículos...");

        controller.getCarsByCategory(category, new CarController.CarCallback() {
            @Override
            public void onSuccess(List<Car> list) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    cars.clear();
                    cars.addAll(list);
                    tvCarCount.setText(cars.size() + " vehículos encontrados");

                    CarAdapter adapter = new CarAdapter(
                            CarListByCategoryActivity.this,
                            cars,
                            (car, position) -> {
                                Intent intent = new Intent(
                                        CarListByCategoryActivity.this,
                                        CarDetailActivity.class);
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
                                overridePendingTransition(
                                        android.R.anim.fade_in,
                                        android.R.anim.fade_out);
                            });

                    rvCars.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvCarCount.setText("Error al cargar");
                    Toast.makeText(CarListByCategoryActivity.this,
                            "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
