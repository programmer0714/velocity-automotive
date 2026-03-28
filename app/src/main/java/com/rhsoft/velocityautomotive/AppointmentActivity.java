package com.rhsoft.velocityautomotive;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppointmentActivity extends AppCompatActivity {

    private TextView tvSelectedDate, tvSelectedTime;
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedType = "Ver";
    private int carId;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        // Datos del carro
        carId           = getIntent().getIntExtra("car_id", 0);
        String carBrand = getIntent().getStringExtra("car_brand");
        String carModel = getIntent().getStringExtra("car_model");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        // Bind views
        ImageView ivCar   = findViewById(R.id.ivCarAppointment);
        TextView tvBrand  = findViewById(R.id.tvCarBrandAppointment);
        TextView tvModel  = findViewById(R.id.tvCarModelAppointment);
        tvSelectedDate    = findViewById(R.id.tvSelectedDate);
        tvSelectedTime    = findViewById(R.id.tvSelectedTime);
        Button btnConfirm = findViewById(R.id.btnConfirmarCita);
        EditText etNota   = findViewById(R.id.etNota);

        // Asignar datos del carro
        if (carBrand != null) tvBrand.setText(carBrand);
        if (carModel != null) tvModel.setText(carModel);
        Glide.with(this).load(imageUrl).centerCrop().into(ivCar);

        // Botón atrás
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chips tipo de cita
        setupChips();

        // Selector de fecha
        findViewById(R.id.btnPickDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                // Guardar en formato DD/MM/YYYY para mostrar
                selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year);
                tvSelectedDate.setText(selectedDate);
                tvSelectedDate.setTextColor(getColor(R.color.text_primary));
            },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Selector de hora
        findViewById(R.id.btnPickTime).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedTime = String.format("%02d:%02d", hour, minute);
                tvSelectedTime.setText(selectedTime);
                tvSelectedTime.setTextColor(getColor(R.color.text_primary));
            },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true).show();
        });

        // Confirmar cita
        btnConfirm.setOnClickListener(v -> {
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Selecciona una fecha", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTime.isEmpty()) {
                Toast.makeText(this, "Selecciona una hora", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userId == -1) {
                Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
                return;
            }

            String nota = etNota.getText().toString().trim();
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Guardando...");

            saveAppointmentToSupabase(nota, btnConfirm);
        });
    }

    // ============================================
    // GUARDAR CITA EN SUPABASE
    // ============================================
    private void saveAppointmentToSupabase(String nota, Button btnConfirm) {
        new Thread(() -> {
            try {
                // Convertir fecha DD/MM/YYYY → YYYY-MM-DD para Supabase
                String[] parts = selectedDate.split("/");
                String fechaSupabase = parts[2] + "-" + parts[1] + "-" + parts[0];

                String url = ApiClient.SUPABASE_URL + "/rest/v1/appointments";
                Log.d("VELOCITY", "➡️ Appointment URL: " + url);

                JSONObject json = new JSONObject();
                json.put("user_id", userId);
                json.put("car_id",  carId);
                json.put("tipo",    selectedType);
                json.put("fecha",   fechaSupabase);
                json.put("hora",    selectedTime + ":00");
                json.put("nota",    nota);
                json.put("estado",  "Pendiente");

                Log.d("VELOCITY", "➡️ Appointment data: " + json.toString());

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("apikey", ApiClient.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + ApiClient.SUPABASE_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .build();

                Response response = client.newCall(request).execute();
                Log.d("VELOCITY", "⬅️ Appointment código: " + response.code());

                if (response.code() == 201 || response.code() == 200) {
                    runOnUiThread(() -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("CONFIRMAR CITA");
                        Toast.makeText(this,
                                "✅ Cita agendada para el " + selectedDate + " a las " + selectedTime,
                                Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    String respBody = response.body().string();
                    Log.e("VELOCITY", "❌ Error cita: " + respBody);
                    runOnUiThread(() -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("CONFIRMAR CITA");
                        Toast.makeText(this, "Error al guardar la cita", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Exception cita: " + e.getMessage());
                runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("CONFIRMAR CITA");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setupChips() {
        TextView chipVer    = findViewById(R.id.chipVer);
        TextView chipPrueba = findViewById(R.id.chipPrueba);
        TextView chipCompra = findViewById(R.id.chipCompra);

        TextView[] chips = {chipVer, chipPrueba, chipCompra};
        String[]   types = {"Ver", "Test Drive", "Compra"};

        for (int i = 0; i < chips.length; i++) {
            final String type = types[i];
            chips[i].setOnClickListener(v -> {
                for (TextView c : chips) {
                    c.setSelected(false);
                    c.setTextColor(getColor(R.color.text_secondary));
                }
                ((TextView) v).setSelected(true);
                ((TextView) v).setTextColor(getColor(R.color.bg_dark));
                selectedType = type;
            });
        }
    }
}