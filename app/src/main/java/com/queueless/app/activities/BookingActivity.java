package com.queueless.app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.queueless.app.databinding.ActivityBookingBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.models.Office;
import com.queueless.app.models.OfficeService;
import com.queueless.app.models.QueueCounter;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;
import com.queueless.app.utils.DateTimeUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Office selectedOffice;
    private OfficeService selectedService;
    private User currentCustomer;

    private String selectedDate = "";
    private String selectedTimeSlot = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        selectedOffice = (Office) getIntent().getSerializableExtra("office");
        selectedService = (OfficeService) getIntent().getSerializableExtra("service");

        if (selectedOffice == null || selectedService == null) {
            Toast.makeText(this, "Office or service details missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCustomerProfile();
        setupViews();
    }

    private void loadCustomerProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentCustomer = doc.toObject(User.class);
                        }
                    });
        }
    }

    private void setupViews() {
        binding.btnBookingBack.setOnClickListener(v -> finish());

        // Header and Summary
        binding.tvBookingOfficeName.setText(selectedOffice.getName());
        binding.tvBookingServiceName.setText(selectedService.getName());
        binding.tvBookingDuration.setText("⏱ Avg Service: " + selectedService.getAverageServiceMinutes() + " mins");
        binding.tvBookingCapacity.setText("Daily Capacity: " + selectedService.getDailyCapacity());

        // Configure available booking modes
        String mode = selectedService.getBookingMode();
        if ("INSTANT_ONLY".equalsIgnoreCase(mode)) {
            binding.rbInstantToken.setVisibility(View.VISIBLE);
            binding.rbScheduledAppointment.setVisibility(View.GONE);
            binding.rbInstantToken.setChecked(true);
            showInstantContainer();
        } else if ("APPOINTMENT_ONLY".equalsIgnoreCase(mode)) {
            binding.rbInstantToken.setVisibility(View.GONE);
            binding.rbScheduledAppointment.setVisibility(View.VISIBLE);
            binding.rbScheduledAppointment.setChecked(true);
            showScheduledContainer();
        } else {
            binding.rbInstantToken.setVisibility(View.VISIBLE);
            binding.rbScheduledAppointment.setVisibility(View.VISIBLE);
            binding.rbInstantToken.setChecked(true);
            showInstantContainer();
        }

        // Toggle between Instant and Scheduled
        binding.rgBookingMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.rbInstantToken.getId()) {
                showInstantContainer();
            } else {
                showScheduledContainer();
            }
        });

        // Date Picker for Scheduled
        binding.etAppointmentDate.setOnClickListener(v -> showDatePicker());

        // Chip selection for Time Slots
        binding.chipGroupTimeSlots.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedTimeSlot = chip.getText().toString();
                }
            } else {
                selectedTimeSlot = "";
            }
        });

        // Action button
        binding.btnConfirmBooking.setOnClickListener(v -> {
            if (binding.rbInstantToken.isChecked()) {
                executeInstantTokenTransaction();
            } else {
                executeScheduledAppointmentBooking();
            }
        });
    }

    private void showInstantContainer() {
        binding.containerInstantToken.setVisibility(View.VISIBLE);
        binding.containerScheduledAppointment.setVisibility(View.GONE);
        binding.btnConfirmBooking.setText("Generate Instant Token");
    }

    private void showScheduledContainer() {
        binding.containerInstantToken.setVisibility(View.GONE);
        binding.containerScheduledAppointment.setVisibility(View.VISIBLE);
        binding.btnConfirmBooking.setText("Confirm Appointment Booking");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    binding.etAppointmentDate.setText(DateTimeUtils.formatTimestampToDate(selectedCal.getTimeInMillis()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Prevent past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Executes atomic Firestore transaction for Instant Token Generation.
     * Prevents duplicate tokens and verifies daily capacity concurrently.
     */
    private void executeInstantTokenTransaction() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Please sign in to generate a token.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String todayDate = DateTimeUtils.getTodayDateString();
        String counterDocId = selectedOffice.getOfficeId() + "_" + todayDate;
        DocumentReference counterRef = db.collection(Constants.COLLECTION_QUEUE_COUNTERS).document(counterDocId);
        DocumentReference newAppointmentRef = db.collection(Constants.COLLECTION_APPOINTMENTS).document();

        db.runTransaction(transaction -> {
            DocumentSnapshot counterDoc = transaction.get(counterRef);

            long nextSequence;
            if (!counterDoc.exists()) {
                nextSequence = 1;
                QueueCounter newCounter = new QueueCounter(selectedOffice.getOfficeId(), todayDate, 1, 0);
                transaction.set(counterRef, newCounter);
            } else {
                Long currentLastNumber = counterDoc.getLong("lastIssuedNumber");
                long lastNumber = currentLastNumber != null ? currentLastNumber : 0;

                // Validate capacity
                if (lastNumber >= selectedService.getDailyCapacity()) {
                    throw new FirebaseFirestoreException(
                            "Daily queue capacity of " + selectedService.getDailyCapacity() + " reached for today.",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }

                nextSequence = lastNumber + 1;
                transaction.update(counterRef, "lastIssuedNumber", nextSequence);
            }

            // Derive Token String (e.g. A001)
            String prefix = selectedService.getName().substring(0, 1).toUpperCase();
            String tokenNumber = DateTimeUtils.formatTokenNumber(prefix, nextSequence);

            String customerName = currentCustomer != null ? currentCustomer.getName() : (firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Customer");
            String customerEmail = currentCustomer != null ? currentCustomer.getEmail() : firebaseUser.getEmail();

            Appointment newAppointment = new Appointment(
                    newAppointmentRef.getId(),
                    firebaseUser.getUid(),
                    customerName,
                    customerEmail,
                    selectedOffice.getOfficeId(),
                    selectedOffice.getName(),
                    selectedService.getServiceId(),
                    selectedService.getName(),
                    Constants.MODE_INSTANT,
                    todayDate,
                    "", // Instant token has no future slot time
                    tokenNumber,
                    nextSequence,
                    Constants.STATUS_WAITING,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
            );

            transaction.set(newAppointmentRef, newAppointment);
            return newAppointmentRef.getId();
        }).addOnSuccessListener(appointmentId -> {
            setLoading(false);
            Toast.makeText(BookingActivity.this, "Instant queue token generated!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BookingActivity.this, MyTokenActivity.class);
            intent.putExtra("appointmentId", appointmentId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(BookingActivity.this, "Could not generate token: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Books a future appointment after validating date and slot capacity.
     */
    private void executeScheduledAppointmentBooking() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Please sign in to book an appointment.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty()) {
            binding.tilAppointmentDate.setError("Please choose an appointment date");
            return;
        }
        binding.tilAppointmentDate.setError(null);

        if (selectedTimeSlot.isEmpty()) {
            Toast.makeText(this, "Please select an appointment time slot.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Check double booking / slot capacity (Max 5 appointments per slot for this service)
        db.collection(Constants.COLLECTION_APPOINTMENTS)
                .whereEqualTo("officeId", selectedOffice.getOfficeId())
                .whereEqualTo("serviceId", selectedService.getServiceId())
                .whereEqualTo("appointmentDate", selectedDate)
                .whereEqualTo("appointmentTime", selectedTimeSlot)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeInSlot = 0;
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String status = doc.getString("status");
                            if (!Constants.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                                activeInSlot++;
                            }
                        }
                    }

                    int slotMaxCapacity = 5;
                    if (activeInSlot >= slotMaxCapacity) {
                        setLoading(false);
                        Toast.makeText(BookingActivity.this, "Selected time slot (" + selectedTimeSlot + ") is fully booked. Please choose another slot.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Create scheduled appointment
                    DocumentReference newApptRef = db.collection(Constants.COLLECTION_APPOINTMENTS).document();
                    String customerName = currentCustomer != null ? currentCustomer.getName() : (firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Customer");
                    String customerEmail = currentCustomer != null ? currentCustomer.getEmail() : firebaseUser.getEmail();

                    Appointment appointment = new Appointment(
                            newApptRef.getId(),
                            firebaseUser.getUid(),
                            customerName,
                            customerEmail,
                            selectedOffice.getOfficeId(),
                            selectedOffice.getName(),
                            selectedService.getServiceId(),
                            selectedService.getName(),
                            Constants.MODE_SCHEDULED,
                            selectedDate,
                            selectedTimeSlot,
                            "APT-" + (activeInSlot + 1),
                            0, // Queue sequence is 0 until checked-in
                            Constants.STATUS_BOOKED,
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                    );

                    newApptRef.set(appointment)
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                Toast.makeText(BookingActivity.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(BookingActivity.this, MyTokenActivity.class);
                                intent.putExtra("appointmentId", newApptRef.getId());
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(BookingActivity.this, "Failed to save appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(BookingActivity.this, "Error validating slot capacity: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        binding.pbBooking.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnConfirmBooking.setEnabled(!isLoading);
    }
}
