package com.queueless.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.queueless.app.R;
import com.queueless.app.databinding.ActivityMyTokenBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.models.QueueCounter;
import com.queueless.app.utils.Constants;
import com.queueless.app.utils.DateTimeUtils;

public class MyTokenActivity extends AppCompatActivity {

    private ActivityMyTokenBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String appointmentId;
    private Appointment currentAppointment;
    private ListenerRegistration appointmentListener;
    private ListenerRegistration queuePositionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyTokenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        binding.btnTokenBack.setOnClickListener(v -> finish());

        appointmentId = getIntent().getStringExtra("appointmentId");

        if (appointmentId != null && !appointmentId.isEmpty()) {
            listenToAppointment(appointmentId);
        } else {
            // Find most recent appointment for this customer
            findLatestCustomerAppointment();
        }

        binding.btnCancelBooking.setOnClickListener(v -> promptCancellation());
        binding.btnCheckIn.setOnClickListener(v -> performCheckIn());
    }

    private void findLatestCustomerAppointment() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("customerId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot latestDoc = null;
                        long latestTimestamp = -1;
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Long created = doc.getLong("createdAt");
                            if (created != null && created > latestTimestamp) {
                                latestTimestamp = created;
                                latestDoc = doc;
                            }
                        }

                        if (latestDoc != null) {
                            appointmentId = latestDoc.getId();
                            listenToAppointment(appointmentId);
                            return;
                        }
                    }

                    Toast.makeText(this, "No active tokens or bookings found.", Toast.LENGTH_LONG).show();
                    binding.tvTokenNumberLarge.setText("---");
                    binding.tvTokenStatusBadge.setText("NO ACTIVE TOKEN");
                    binding.btnCancelBooking.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load token: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenToAppointment(String apptId) {
        DocumentReference apptRef = db.collection(Constants.COLLECTION_APPOINTMENTS).document(apptId);

        appointmentListener = apptRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error syncing token: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentAppointment = snapshot.toObject(Appointment.class);
                if (currentAppointment != null) {
                    displayAppointmentDetails(currentAppointment);
                    listenToQueuePosition(currentAppointment);
                }
            }
        });
    }

    private void displayAppointmentDetails(Appointment appt) {
        binding.tvTokenOfficeName.setText(appt.getOfficeName());
        binding.tvTokenServiceName.setText(appt.getServiceName());
        binding.tvTokenNumberLarge.setText(appt.getTokenNumber());

        String dateStr = appt.getAppointmentDate();
        if (appt.getAppointmentTime() != null && !appt.getAppointmentTime().isEmpty()) {
            dateStr += " • " + appt.getAppointmentTime();
        }
        binding.tvBookingDateTime.setText("Scheduled for: " + dateStr);

        // Update status badge styling
        String status = appt.getStatus();
        binding.tvTokenStatusBadge.setText(status);

        if (Constants.STATUS_SERVING.equalsIgnoreCase(status)) {
            binding.tvTokenStatusBadge.setTextColor(getColor(R.color.status_serving));
            binding.btnCancelBooking.setVisibility(View.GONE);
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.tvPeopleAheadCount.setText("0 (Now Serving!)");
            binding.tvEstimatedWaitTime.setText("0 min");
        } else if (Constants.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            binding.tvTokenStatusBadge.setTextColor(getColor(R.color.status_completed));
            binding.btnCancelBooking.setVisibility(View.GONE);
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.containerLiveQueueStats.setVisibility(View.GONE);
            binding.cardNowServingBanner.setVisibility(View.GONE);
        } else if (Constants.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            binding.tvTokenStatusBadge.setTextColor(getColor(R.color.status_cancelled));
            binding.btnCancelBooking.setVisibility(View.GONE);
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.containerLiveQueueStats.setVisibility(View.GONE);
            binding.cardNowServingBanner.setVisibility(View.GONE);
        } else if (Constants.STATUS_SKIPPED.equalsIgnoreCase(status)) {
            binding.tvTokenStatusBadge.setTextColor(getColor(R.color.status_skipped));
            binding.btnCancelBooking.setVisibility(View.GONE);
            binding.btnCheckIn.setVisibility(View.GONE);
        } else if (Constants.STATUS_BOOKED.equalsIgnoreCase(status)) {
            binding.tvTokenStatusBadge.setTextColor(getColor(R.color.primary));
            binding.btnCancelBooking.setVisibility(View.VISIBLE);
            // Allow check-in if appointment is today
            String today = DateTimeUtils.getTodayDateString();
            if (today.equalsIgnoreCase(appt.getAppointmentDate())) {
                binding.btnCheckIn.setVisibility(View.VISIBLE);
            } else {
                binding.btnCheckIn.setVisibility(View.GONE);
            }
        } else {
            // WAITING status
            binding.tvTokenStatusBadge.setTextColor(getColor(R.color.status_waiting));
            binding.btnCancelBooking.setVisibility(View.VISIBLE);
            binding.btnCheckIn.setVisibility(View.GONE);
        }
    }

    private void listenToQueuePosition(Appointment appt) {
        if (!Constants.STATUS_WAITING.equalsIgnoreCase(appt.getStatus())) {
            return;
        }

        if (queuePositionListener != null) {
            queuePositionListener.remove();
        }

        // Realtime query for other tokens in the same queue
        queuePositionListener = db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("officeId", appt.getOfficeId())
                .whereEqualTo("appointmentDate", appt.getAppointmentDate())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    int peopleAhead = 0;
                    String servingToken = "None";

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Appointment other = doc.toObject(Appointment.class);
                        if (Constants.STATUS_WAITING.equalsIgnoreCase(other.getStatus())) {
                            if (other.getQueueSequence() < appt.getQueueSequence()) {
                                peopleAhead++;
                            }
                        } else if (Constants.STATUS_SERVING.equalsIgnoreCase(other.getStatus())) {
                            servingToken = other.getTokenNumber();
                        }
                    }

                    binding.tvPeopleAheadCount.setText(String.valueOf(peopleAhead));
                    binding.tvCurrentServingToken.setText(servingToken);

                    // Estimate: 5 mins default or 5 * people ahead
                    int estimatedWaitMinutes = (peopleAhead + 1) * 5;
                    binding.tvEstimatedWaitTime.setText("~" + estimatedWaitMinutes + " mins");
                });
    }

    private void performCheckIn() {
        if (currentAppointment == null) return;

        String today = DateTimeUtils.getTodayDateString();
        String counterDocId = currentAppointment.getOfficeId() + "_" + today;
        DocumentReference counterRef = db.collection(Constants.COLLECTION_QUEUE_COUNTERS).document(counterDocId);
        DocumentReference apptRef = db.collection(Constants.COLLECTION_APPOINTMENTS).document(currentAppointment.getAppointmentId());

        db.runTransaction(transaction -> {
            DocumentSnapshot counterDoc = transaction.get(counterRef);
            long nextSequence = 1;
            if (counterDoc.exists() && counterDoc.getLong("lastIssuedNumber") != null) {
                nextSequence = counterDoc.getLong("lastIssuedNumber") + 1;
                transaction.update(counterRef, "lastIssuedNumber", nextSequence);
            } else {
                transaction.set(counterRef, new QueueCounter(currentAppointment.getOfficeId(), today, 1, 0));
            }

            String prefix = currentAppointment.getServiceName().substring(0, 1).toUpperCase();
            String tokenNumber = DateTimeUtils.formatTokenNumber(prefix, nextSequence);

            transaction.update(apptRef,
                    "status", Constants.STATUS_WAITING,
                    "queueSequence", nextSequence,
                    "tokenNumber", tokenNumber,
                    "updatedAt", System.currentTimeMillis()
            );
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Checked in! You are now in the live queue.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Check-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void promptCancellation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking/token? This cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelBooking())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelBooking() {
        if (currentAppointment == null) return;

        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .document(currentAppointment.getAppointmentId())
                .update("status", Constants.STATUS_CANCELLED, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking cancelled successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not cancel booking: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentListener != null) {
            appointmentListener.remove();
        }
        if (queuePositionListener != null) {
            queuePositionListener.remove();
        }
    }
}
