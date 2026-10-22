package com.queueless.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.queueless.app.R;
import com.queueless.app.databinding.ActivityAdminDashboardBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.models.Office;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;
import com.queueless.app.utils.DateTimeUtils;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration queueMetricsListener;
    private String officeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Verify ADMIN authorization securely
        verifyAdminAuthorization(currentUser.getUid());

        binding.btnAdminSignOut.setOnClickListener(v -> signOut());

        binding.cardAdminQueueManager.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminQueueActivity.class);
            if (officeId != null) {
                intent.putExtra("officeId", officeId);
            }
            startActivity(intent);
        });

        binding.cardAdminManageSlots.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageSlotsActivity.class);
            if (officeId != null) {
                intent.putExtra("officeId", officeId);
            }
            startActivity(intent);
        });
    }

    private void verifyAdminAuthorization(String uid) {
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user == null || !Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                            // Security check failed: user is not an administrator
                            Toast.makeText(this, getString(R.string.unauthorized_access), Toast.LENGTH_LONG).show();
                            redirectToLogin();
                            return;
                        }

                        binding.tvAdminEmail.setText(user.getEmail());
                        this.officeId = user.getOfficeId();

                        if (officeId != null && !officeId.trim().isEmpty()) {
                            loadOfficeDetails(officeId);
                            listenForQueueMetrics(officeId);
                        } else {
                            binding.tvAdminOfficeName.setText("Office Not Assigned");
                            binding.tvAdminOfficeCode.setText("CODE: N/A");
                            Toast.makeText(this, "Notice: No office ID is assigned to this admin account.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        redirectToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Authorization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                });
    }

    private void loadOfficeDetails(String officeId) {
        db.collection(Constants.COLLECTION_OFFICES)
                .document(officeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Office office = documentSnapshot.toObject(Office.class);
                        if (office != null) {
                            binding.tvAdminOfficeName.setText(office.getName());
                            binding.tvAdminOfficeCode.setText("CODE: " + office.getOfficeCode());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load office details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForQueueMetrics(String officeId) {
        String today = DateTimeUtils.getTodayDateString();

        // Realtime query for all appointments today for this office
        queueMetricsListener = db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("officeId", officeId)
                .whereEqualTo("appointmentDate", today)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        int waitingCount = 0;
                        int totalToday = queryDocumentSnapshots.size();
                        Appointment currentlyServing = null;

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (Constants.STATUS_WAITING.equalsIgnoreCase(appt.getStatus())) {
                                waitingCount++;
                            } else if (Constants.STATUS_SERVING.equalsIgnoreCase(appt.getStatus())) {
                                currentlyServing = appt;
                            }
                        }

                        // Update dashboard UI
                        binding.tvWaitingCount.setText(String.valueOf(waitingCount));
                        binding.tvTotalCount.setText(String.valueOf(totalToday));

                        if (currentlyServing != null) {
                            binding.tvServingTokenNumber.setText(currentlyServing.getTokenNumber());
                            binding.tvServingCustomerDetails.setText(
                                    (currentlyServing.getCustomerName() != null ? currentlyServing.getCustomerName() : "Customer") +
                                    " • " + currentlyServing.getServiceName()
                            );
                        } else {
                            binding.tvServingTokenNumber.setText("None");
                            binding.tvServingCustomerDetails.setText("No customer is currently called");
                        }
                    }
                });
    }

    private void signOut() {
        mAuth.signOut();
        Toast.makeText(this, "Admin signed out", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueMetricsListener != null) {
            queueMetricsListener.remove();
        }
    }
}
