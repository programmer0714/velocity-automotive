package com.rhsoft.velocityautomotive;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoContainer = findViewById(R.id.logoContainer);
        View subtitleText  = findViewById(R.id.subtitleText);

        // Animación fade + slide del logo
        ObjectAnimator fadeInLogo = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f);
        fadeInLogo.setDuration(1200);
        fadeInLogo.setStartDelay(300);

        ObjectAnimator slideUp = ObjectAnimator.ofFloat(logoContainer, "translationY", 60f, 0f);
        slideUp.setDuration(1000);
        slideUp.setStartDelay(300);
        slideUp.setInterpolator(new AccelerateDecelerateInterpolator());

        // Animación fade del subtítulo
        ObjectAnimator fadeInSub = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f);
        fadeInSub.setDuration(800);
        fadeInSub.setStartDelay(1200);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeInLogo, slideUp, fadeInSub);
        set.start();

        // Ir al Login después de 3.5 segundos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3500);
    }
}
