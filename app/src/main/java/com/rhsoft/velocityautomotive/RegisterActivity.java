package com.rhsoft.velocityautomotive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView btnGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail           = findViewById(R.id.etEmail);
        etPhone           = findViewById(R.id.etPhone);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        btnGoLogin        = findViewById(R.id.btnGoLogin);

        btnRegister.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String telefono = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirm  = etConfirmPassword.getText().toString().trim();

            // Generar nombre desde email
            String nombre = email.contains("@")
                    ? email.substring(0, email.indexOf("@"))
                    : email;
            nombre = nombre.substring(0, 1).toUpperCase() + nombre.substring(1);

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Registrando...");

            final String finalNombre = nombre;
            registerWithSupabase(finalNombre, email, telefono, password);
        });

        btnGoLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    private void registerWithSupabase(String nombre, String email, String telefono, String password) {
        new Thread(() -> {
            try {
                String url = ApiClient.SUPABASE_URL + "/rest/v1/users";
                Log.d("VELOCITY", "➡️ Register URL: " + url);

                JSONObject json = new JSONObject();
                json.put("nombre",   nombre);
                json.put("email",    email);
                json.put("telefono", telefono);
                json.put("password", password);

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
                        .header("Prefer", "return=representation")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d("VELOCITY", "⬅️ Register código: " + response.code());
                Log.d("VELOCITY", "⬅️ Register respuesta: " + responseBody);

                if (response.code() == 201 || response.code() == 200) {
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("CREAR CUENTA");

                        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_name",  nombre)
                                .putString("user_email", email)
                                .putString("user_phone", telefono)
                                .putBoolean("is_logged", true)
                                .apply();

                        Log.d("VELOCITY", "✅ Registro exitoso: " + nombre);
                        goToMain();
                    });
                } else {
                    String errorMsg = "Error al registrar";
                    if (responseBody.contains("duplicate") || responseBody.contains("unique")) {
                        errorMsg = "El email ya está registrado";
                    }
                    final String finalError = errorMsg;
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("CREAR CUENTA");
                        Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error register: " + e.getMessage());
                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("CREAR CUENTA");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}