package com.queueless.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.queueless.app.R;
import com.queueless.app.databinding.ActivityLoginBinding;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        // Reset previous errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        // Validation
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_fields));
            binding.etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            binding.etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_empty_fields));
            binding.etPassword.requestFocus();
            return;
        }

        // Show loading state
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            fetchUserRoleAndNavigate(firebaseUser.getUid());
                        } else {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(LoginActivity.this, "Login Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                            Toast.makeText(this, "Signed in as Office Administrator", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        // User profile document was missing in Firestore (e.g. earlier permission issue)
                        // Self-heal: create standard customer profile and proceed
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String fallbackName = (firebaseUser != null && firebaseUser.getDisplayName() != null) ? firebaseUser.getDisplayName() : "Customer";
                        String fallbackEmail = (firebaseUser != null && firebaseUser.getEmail() != null) ? firebaseUser.getEmail() : "";

                        User fallbackUser = new User(uid, fallbackName, fallbackEmail, Constants.ROLE_CUSTOMER, null, System.currentTimeMillis());
                        db.collection(Constants.COLLECTION_USERS).document(uid).set(fallbackUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Profile initialized successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "User profile not found. Please contact support.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to load user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.pbLogin.setVisibility(View.VISIBLE);
            binding.btnLogin.setEnabled(false);
            binding.tvGoToRegister.setEnabled(false);
        } else {
            binding.pbLogin.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
            binding.tvGoToRegister.setEnabled(true);
        }
    }
}
