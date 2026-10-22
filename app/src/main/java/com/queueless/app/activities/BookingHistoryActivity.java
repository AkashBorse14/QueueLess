package com.queueless.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.queueless.app.adapters.BookingHistoryAdapter;
import com.queueless.app.databinding.ActivityBookingHistoryBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {

    private ActivityBookingHistoryBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BookingHistoryAdapter adapter;
    private ListenerRegistration historyListener;

    private final List<Appointment> allAppointments = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupUI();
        loadBookingHistory();
    }

    private void setupUI() {
        binding.btnHistoryBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new BookingHistoryAdapter(appointment -> {
            Intent intent = new Intent(BookingHistoryActivity.this, MyTokenActivity.class);
            intent.putExtra("appointmentId", appointment.getAppointmentId());
            startActivity(intent);
        });

        binding.rvBookingHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBookingHistory.setAdapter(adapter);

        // Filter chips
        binding.chipGroupHistoryFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == binding.chipFilterActive.getId()) {
                currentFilter = "ACTIVE";
            } else if (checkedId == binding.chipFilterCompleted.getId()) {
                currentFilter = "COMPLETED";
            } else if (checkedId == binding.chipFilterCancelled.getId()) {
                currentFilter = "CANCELLED";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void loadBookingHistory() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in to view history.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.pbHistory.setVisibility(View.VISIBLE);

        // Realtime query for this customer's bookings (in-memory sort avoids composite index requirement)
        historyListener = db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("customerId", user.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    binding.pbHistory.setVisibility(View.GONE);

                    if (e != null) {
                        Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allAppointments.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Appointment appt = doc.toObject(Appointment.class);
                            allAppointments.add(appt);
                        }
                    }
                    // Sort descending by createdAt (newest first)
                    java.util.Collections.sort(allAppointments, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    applyFilter();
                });
    }

    private void applyFilter() {
        List<Appointment> filteredList = new ArrayList<>();

        for (Appointment appt : allAppointments) {
            String status = appt.getStatus();
            switch (currentFilter) {
                case "ACTIVE":
                    if (Constants.STATUS_WAITING.equalsIgnoreCase(status) ||
                        Constants.STATUS_SERVING.equalsIgnoreCase(status) ||
                        Constants.STATUS_BOOKED.equalsIgnoreCase(status)) {
                        filteredList.add(appt);
                    }
                    break;
                case "COMPLETED":
                    if (Constants.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                        filteredList.add(appt);
                    }
                    break;
                case "CANCELLED":
                    if (Constants.STATUS_CANCELLED.equalsIgnoreCase(status) ||
                        Constants.STATUS_SKIPPED.equalsIgnoreCase(status)) {
                        filteredList.add(appt);
                    }
                    break;
                default:
                    // "ALL"
                    filteredList.add(appt);
                    break;
            }
        }

        adapter.setAppointments(filteredList);

        if (filteredList.isEmpty()) {
            binding.containerEmptyHistory.setVisibility(View.VISIBLE);
            binding.rvBookingHistory.setVisibility(View.GONE);
        } else {
            binding.containerEmptyHistory.setVisibility(View.GONE);
            binding.rvBookingHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) {
            historyListener.remove();
        }
    }
}
