package com.queueless.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.queueless.app.R;
import com.queueless.app.databinding.ActivityCustomerDashboardBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;
import com.queueless.app.utils.DateTimeUtils;

public class CustomerDashboardActivity extends AppCompatActivity {

    private ActivityCustomerDashboardBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration activeTicketListener;
    private String currentActiveAppointmentId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Verify authorized role & display customer info
        loadCustomerProfile(currentUser.getUid());

        // Setup click listeners
        setupClickListeners();

        // Listen for today's active ticket
        listenForActiveTicket(currentUser.getUid());
    }

    private void loadCustomerProfile(String uid) {
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // If an admin somehow lands here, redirect to AdminDashboard
                            if (Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                                Intent intent = new Intent(CustomerDashboardActivity.this, AdminDashboardActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                            binding.tvCustomerName.setText("Hello, " + user.getName());
                            binding.tvCustomerEmail.setText(user.getEmail());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners() {
        // Sign Out
        binding.btnSignOut.setOnClickListener(v -> signOut());

        // Connect to Office
        binding.cardConnectOffice.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, OfficeConnectActivity.class);
            startActivity(intent);
        });

        // Active Queue Token
        binding.cardMyTokens.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, MyTokenActivity.class);
            if (currentActiveAppointmentId != null) {
                intent.putExtra("appointmentId", currentActiveAppointmentId);
            }
            startActivity(intent);
        });

        // Booking History
        binding.cardBookingHistory.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, BookingHistoryActivity.class);
            startActivity(intent);
        });

        // Banner button
        binding.btnViewActiveToken.setOnClickListener(v -> {
            if (currentActiveAppointmentId != null) {
                Intent intent = new Intent(CustomerDashboardActivity.this, MyTokenActivity.class);
                intent.putExtra("appointmentId", currentActiveAppointmentId);
                startActivity(intent);
            }
        });
    }

    private void listenForActiveTicket(String uid) {
        String today = DateTimeUtils.getTodayDateString();

        // Realtime query for active tickets today
        activeTicketListener = db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("customerId", uid)
                .whereEqualTo("appointmentDate", today)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Appointment activeAppointment = null;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Appointment appt = doc.toObject(Appointment.class);
                            // Look for WAITING or SERVING statuses
                            if (Constants.STATUS_WAITING.equalsIgnoreCase(appt.getStatus()) ||
                                Constants.STATUS_SERVING.equalsIgnoreCase(appt.getStatus())) {
                                activeAppointment = appt;
                                break;
                            }
                        }

                        if (activeAppointment != null) {
                            currentActiveAppointmentId = activeAppointment.getAppointmentId();
                            binding.cardActiveTokenAlert.setVisibility(View.VISIBLE);
                            binding.tvActiveTokenNumber.setText("Token #" + activeAppointment.getTokenNumber());
                            binding.tvActiveOfficeName.setText(activeAppointment.getOfficeName() + " • " + activeAppointment.getServiceName());
                            binding.tvActiveTokenStatus.setText(activeAppointment.getStatus());

                            if (Constants.STATUS_SERVING.equalsIgnoreCase(activeAppointment.getStatus())) {
                                binding.tvActiveTokenStatus.setTextColor(getColor(R.color.status_serving));
                            } else {
                                binding.tvActiveTokenStatus.setTextColor(getColor(R.color.status_waiting));
                            }
                        } else {
                            currentActiveAppointmentId = null;
                            binding.cardActiveTokenAlert.setVisibility(View.GONE);
                        }
                    } else {
                        currentActiveAppointmentId = null;
                        binding.cardActiveTokenAlert.setVisibility(View.GONE);
                    }
                });
    }

    private void signOut() {
        mAuth.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(CustomerDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeTicketListener != null) {
            activeTicketListener.remove();
        }
    }
}
