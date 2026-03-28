package com.rhsoft.velocityautomotive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.rhsoft.velocityautomotive.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGuest;
    private TextView btnGoRegister, tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        btnGuest      = findViewById(R.id.btnGuest);
        btnGoRegister = findViewById(R.id.btnGoRegister);
        tvError       = findViewById(R.id.tvError);

        // Animaciones de entrada al abrir la pantalla
        startAnimations();

        // Botón iniciar sesión — valida campos y consulta Supabase
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                showError("Completa todos los campos");
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Entrando...");
            hideError();
            loginWithSupabase(email, pass);
        });

        // Botón entrar como invitado — guarda sesión guest sin consultar Supabase
        btnGuest.setOnClickListener(v -> enterAsGuest());

        // Botón ir al registro
        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    // ============================================
    // MODO INVITADO
    // Guarda is_guest=true en SharedPreferences
    // sin necesitar email ni contraseña
    // ============================================
    private void enterAsGuest() {
        SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("user_name",  "Invitado")
                .putString("user_email", "")
                .putString("user_phone", "")
                .putInt("user_id",       -1)
                .putBoolean("is_logged", false)
                .putBoolean("is_guest",  true)   // ← marca como invitado
                .apply();

        Log.d("VELOCITY", "✅ Entrando como invitado");
        goToMain();
    }

    // ============================================
    // LOGIN NORMAL CON SUPABASE
    // Busca el usuario por email y verifica password
    // ============================================
    private void loginWithSupabase(String email, String password) {
        new Thread(() -> {
            try {
                String url = ApiClient.SUPABASE_URL + "/rest/v1/users?email=eq." + email + "&select=*";
                Log.d("VELOCITY", "➡️ Login URL: " + url);

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
                Log.d("VELOCITY", "⬅️ Login respuesta: " + responseBody);

                JSONArray users = new JSONArray(responseBody);

                // Usuario no encontrado
                if (users.length() == 0) {
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("INICIAR SESIÓN");
                        showError("Email o contraseña incorrectos");
                    });
                    return;
                }

                JSONObject user = users.getJSONObject(0);
                String storedPassword = user.optString("password", "");

                // Contraseña incorrecta
                if (!storedPassword.equals(password)) {
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("INICIAR SESIÓN");
                        showError("Email o contraseña incorrectos");
                    });
                    return;
                }

                // Login exitoso — guardar sesión en SharedPreferences
                String nombre    = user.optString("nombre",   email);
                String userEmail = user.optString("email",    email);
                String telefono  = user.optString("telefono", "");
                int    userId    = user.optInt("id",          0);

                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("INICIAR SESIÓN");

                    SharedPreferences prefs = getSharedPreferences("velocity_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("user_name",  nombre)
                            .putString("user_email", userEmail)
                            .putString("user_phone", telefono)
                            .putInt("user_id",       userId)
                            .putBoolean("is_logged", true)
                            .putBoolean("is_guest",  false) // ← no es invitado
                            .apply();

                    Log.d("VELOCITY", "✅ Login exitoso: " + nombre);
                    goToMain();
                });

            } catch (Exception e) {
                Log.e("VELOCITY", "❌ Error login: " + e.getMessage());
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("INICIAR SESIÓN");
                    showError("Error de conexión");
                });
            }
        }).start();
    }

    // Animaciones de entrada — logo y carro
    private void startAnimations() {
        ImageView ivLogo = findViewById(R.id.ivLogo);
        ivLogo.setAlpha(0f);
        ivLogo.setTranslationY(-60f);
        ivLogo.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(200).start();

        ImageView ivCar = findViewById(R.id.ivLoginCar);
        if (ivCar != null) {
            ivCar.setAlpha(0f);
            ivCar.setTranslationY(80f);
            ivCar.animate().alpha(0.85f).translationY(0f).setDuration(1000).setStartDelay(400).start();
        }

        btnLogin.setAlpha(0f);
        btnLogin.animate().alpha(1f).setDuration(500).setStartDelay(900).start();

        btnGuest.setAlpha(0f);
        btnGuest.animate().alpha(1f).setDuration(500).setStartDelay(1000).start();
    }

    private void showError(String msg) {
        if (tvError != null) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(msg);
            // Animación shake en el error
            tvError.animate().translationX(-10f).setDuration(50)
                    .withEndAction(() -> tvError.animate().translationX(10f).setDuration(50)
                            .withEndAction(() -> tvError.animate().translationX(0f).setDuration(50).start())
                            .start()).start();
        }
    }

    private void hideError() {
        if (tvError != null) tvError.setVisibility(View.GONE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}