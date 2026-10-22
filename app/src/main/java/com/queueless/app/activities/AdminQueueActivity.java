package com.queueless.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.queueless.app.adapters.AdminQueueAdapter;
import com.queueless.app.databinding.ActivityAdminQueueBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.models.Office;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;
import com.queueless.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdminQueueActivity extends AppCompatActivity {

    private ActivityAdminQueueBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private AdminQueueAdapter adapter;
    private ListenerRegistration queueListener;

    private String officeId;
    private Appointment currentlyServing = null;
    private final List<Appointment> waitingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminQueueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        officeId = getIntent().getStringExtra("officeId");

        setupUI();
        resolveOfficeAndListen();
    }

    private void setupUI() {
        binding.btnQueueBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new AdminQueueAdapter(appointment -> {
            // Optional: click to inspect waiting customer details
            Toast.makeText(this, "Customer: " + appointment.getCustomerName() + " (" + appointment.getTokenNumber() + ")", Toast.LENGTH_SHORT).show();
        });

        binding.rvAdminQueue.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAdminQueue.setAdapter(adapter);

        // Control actions
        binding.btnCallNextCustomer.setOnClickListener(v -> handleCallNext());
        binding.btnMarkCompleted.setOnClickListener(v -> markServingCompleted());
        binding.btnSkipCustomer.setOnClickListener(v -> promptSkipCustomer());
    }

    private void resolveOfficeAndListen() {
        if (officeId != null && !officeId.isEmpty()) {
            loadOfficeName(officeId);
            listenToQueue(officeId);
        } else {
            // Query user profile to get admin's officeId
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Admin not authenticated.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            db.collection(Constants.COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null && user.getOfficeId() != null) {
                                officeId = user.getOfficeId();
                                loadOfficeName(officeId);
                                listenToQueue(officeId);
                            } else {
                                Toast.makeText(this, "No office assigned to admin.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void loadOfficeName(String officeId) {
        db.collection(Constants.COLLECTION_OFFICES)
                .document(officeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Office office = doc.toObject(Office.class);
                        if (office != null) {
                            binding.tvAdminOfficeHeader.setText(office.getName() + " (" + office.getOfficeCode() + ")");
                        }
                    }
                });
    }

    private void listenToQueue(String officeId) {
        String today = DateTimeUtils.getTodayDateString();
        binding.pbAdminQueue.setVisibility(View.VISIBLE);

        // Realtime listener for today's office appointments
        queueListener = db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("officeId", officeId)
                .whereEqualTo("appointmentDate", today)
                .addSnapshotListener((snapshots, e) -> {
                    binding.pbAdminQueue.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Queue sync error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentlyServing = null;
                    waitingList.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (Constants.STATUS_SERVING.equalsIgnoreCase(appt.getStatus())) {
                                currentlyServing = appt;
                            } else if (Constants.STATUS_WAITING.equalsIgnoreCase(appt.getStatus())) {
                                waitingList.add(appt);
                            }
                        }
                    }

                    // Sort waiting customers in queue sequence order (lowest sequence first)
                    Collections.sort(waitingList, Comparator.comparingLong(Appointment::getQueueSequence));

                    updateUI();
                });
    }

    private void updateUI() {
        // Update Serving Card
        if (currentlyServing != null) {
            binding.tvDeskServingToken.setText(currentlyServing.getTokenNumber());
            binding.tvDeskServingCustomer.setText(
                    (currentlyServing.getCustomerName() != null ? currentlyServing.getCustomerName() : "Customer") +
                    " • " + currentlyServing.getServiceName()
            );
            binding.layoutServingActions.setVisibility(View.VISIBLE);
        } else {
            binding.tvDeskServingToken.setText("None");
            binding.tvDeskServingCustomer.setText("Counter is ready for next customer");
            binding.layoutServingActions.setVisibility(View.GONE);
        }

        // Update Waiting List & Count
        binding.tvAdminWaitingHeaderCount.setText(waitingList.size() + " Waiting");
        adapter.setWaitingTokens(waitingList);

        if (waitingList.isEmpty()) {
            binding.containerEmptyQueue.setVisibility(View.VISIBLE);
            binding.rvAdminQueue.setVisibility(View.GONE);
        } else {
            binding.containerEmptyQueue.setVisibility(View.GONE);
            binding.rvAdminQueue.setVisibility(View.VISIBLE);
        }
    }

    private void handleCallNext() {
        if (currentlyServing != null) {
            // A customer is currently serving. Prompt whether to auto-complete and call next.
            new AlertDialog.Builder(this)
                    .setTitle("Current Customer in Progress")
                    .setMessage("Token " + currentlyServing.getTokenNumber() + " is currently serving. Mark completed and call the next customer?")
                    .setPositiveButton("Complete & Call Next", (dialog, which) -> {
                        markServingCompletedAndCallNext();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        callNextEligible();
    }

    private void callNextEligible() {
        if (waitingList.isEmpty()) {
            Toast.makeText(this, "No customers are waiting in the queue.", Toast.LENGTH_SHORT).show();
            return;
        }

        Appointment nextAppointment = waitingList.get(0);
        setLoading(true);

        // Update status of next waiting customer to SERVING
        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .document(nextAppointment.getAppointmentId())
                .update(
                        "status", Constants.STATUS_SERVING,
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Called Token #" + nextAppointment.getTokenNumber() + " to Counter!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to call next customer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markServingCompleted() {
        if (currentlyServing == null) return;

        setLoading(true);
        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .document(currentlyServing.getAppointmentId())
                .update(
                        "status", Constants.STATUS_COMPLETED,
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Service completed for " + currentlyServing.getTokenNumber(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error marking completed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markServingCompletedAndCallNext() {
        if (currentlyServing == null) return;

        setLoading(true);
        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .document(currentlyServing.getAppointmentId())
                .update(
                        "status", Constants.STATUS_COMPLETED,
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    callNextEligible();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void promptSkipCustomer() {
        if (currentlyServing == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Skip Customer")
                .setMessage("Skip token " + currentlyServing.getTokenNumber() + " due to absence? Status will be updated to SKIPPED.")
                .setPositiveButton("Yes, Skip", (dialog, which) -> skipServingCustomer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void skipServingCustomer() {
        if (currentlyServing == null) return;

        setLoading(true);
        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .document(currentlyServing.getAppointmentId())
                .update(
                        "status", Constants.STATUS_SKIPPED,
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Token " + currentlyServing.getTokenNumber() + " marked as SKIPPED.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error skipping customer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        binding.pbAdminQueue.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnCallNextCustomer.setEnabled(!isLoading);
        binding.btnMarkCompleted.setEnabled(!isLoading);
        binding.btnSkipCustomer.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queueListener != null) {
            queueListener.remove();
        }
    }
}
