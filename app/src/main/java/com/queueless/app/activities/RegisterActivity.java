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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.queueless.app.R;
import com.queueless.app.databinding.ActivityRegisterBinding;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnRegister.setOnClickListener(v -> performRegistration());

        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString().trim() : "";

        // Reset errors
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_empty_fields));
            binding.etName.requestFocus();
            return;
        }

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

        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_length));
            binding.etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_match));
            binding.etConfirmPassword.requestFocus();
            return;
        }

        // Show loading state
        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Update display name in Firebase Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            firebaseUser.updateProfile(profileUpdates);

                            // Store user profile in Cloud Firestore (Public registration is ALWAYS ROLE_CUSTOMER)
                            User newUser = new User(
                                    firebaseUser.getUid(),
                                    name,
                                    email,
                                    Constants.ROLE_CUSTOMER,
                                    null,
                                    System.currentTimeMillis()
                            );

                            saveUserToFirestore(newUser);
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(User user) {
        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, CustomerDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.pbRegister.setVisibility(View.VISIBLE);
            binding.btnRegister.setEnabled(false);
            binding.tvGoToLogin.setEnabled(false);
        } else {
            binding.pbRegister.setVisibility(View.GONE);
            binding.btnRegister.setEnabled(true);
            binding.tvGoToLogin.setEnabled(true);
        }
    }
}
