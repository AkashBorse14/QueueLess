package com.queueless.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.queueless.app.databinding.ActivitySplashBinding;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Brief delay for branding display
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserSession, 1200);
    }

    private void checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in with Firebase Auth; check their role from Firestore
            db.collection(Constants.COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null && Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                                navigateTo(AdminDashboardActivity.class);
                            } else {
                                navigateTo(CustomerDashboardActivity.class);
                            }
                        } else {
                            // Document missing: force re-login
                            mAuth.signOut();
                            navigateTo(LoginActivity.class);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Session check error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        navigateTo(LoginActivity.class);
                    });
        } else {
            // Not logged in
            navigateTo(LoginActivity.class);
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(SplashActivity.this, targetActivity);
        startActivity(intent);
        finish();
    }
}
