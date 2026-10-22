package com.queueless.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.queueless.app.databinding.ActivityManageSlotsBinding;
import com.queueless.app.models.Office;
import com.queueless.app.models.OfficeService;
import com.queueless.app.models.User;
import com.queueless.app.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageSlotsActivity extends AppCompatActivity {

    private ActivityManageSlotsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String officeId;
    private final List<OfficeService> servicesList = new ArrayList<>();
    private final List<String> serviceNames = new ArrayList<>();
    private OfficeService selectedService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageSlotsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        officeId = getIntent().getStringExtra("officeId");

        binding.btnManageSlotsBack.setOnClickListener(v -> finish());
        binding.btnSaveSlotConfig.setOnClickListener(v -> saveConfiguration());

        resolveOfficeAndLoadServices();
    }

    private void resolveOfficeAndLoadServices() {
        if (officeId != null && !officeId.isEmpty()) {
            loadOfficeAndServices(officeId);
        } else {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Admin not authenticated.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            db.collection(Constants.COLLECTION_USERS)
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User admin = documentSnapshot.toObject(User.class);
                            if (admin != null && admin.getOfficeId() != null) {
                                officeId = admin.getOfficeId();
                                loadOfficeAndServices(officeId);
                            } else {
                                Toast.makeText(this, "No office assigned to admin.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void loadOfficeAndServices(String officeId) {
        binding.pbManageSlots.setVisibility(View.VISIBLE);

        // Load office name
        db.collection(Constants.COLLECTION_OFFICES)
                .document(officeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Office office = doc.toObject(Office.class);
                        if (office != null) {
                            binding.tvManageOfficeSubheader.setText(office.getName() + " (" + office.getOfficeCode() + ")");
                        }
                    }
                });

        // Load services
        db.collection(Constants.COLLECTION_OFFICES)
                .document(officeId)
                .collection(Constants.COLLECTION_SERVICES)
                .get()
                .addOnSuccessListener(snapshots -> {
                    binding.pbManageSlots.setVisibility(View.GONE);
                    servicesList.clear();
                    serviceNames.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            OfficeService service = doc.toObject(OfficeService.class);
                            if (service != null) {
                                if (service.getServiceId() == null) service.setServiceId(doc.getId());
                                if (service.getOfficeId() == null) service.setOfficeId(officeId);
                                servicesList.add(service);
                                serviceNames.add(service.getName());
                            }
                        }
                    }

                    if (servicesList.size() < 3 && (officeId.contains("COL101") || "OFFICE_COL101".equalsIgnoreCase(officeId))) {
                        servicesList.clear();
                        serviceNames.clear();
                        servicesList.add(new OfficeService("SRV_FEE", officeId, "Fee Clearance & No-Dues", "Verification of term fees and issuance of clearance receipt.", true, 50, 5, "BOTH"));
                        servicesList.add(new OfficeService("SRV_TRANS", officeId, "Transcript & Grade Verification", "Official academic transcript request and stamping.", true, 30, 15, "APPOINTMENT_ONLY"));
                        servicesList.add(new OfficeService("SRV_ID", officeId, "Student ID Card & Pass Issue", "Instant queue for duplicate ID cards and bus passes.", true, 100, 3, "INSTANT_ONLY"));
                        for (OfficeService s : servicesList) {
                            serviceNames.add(s.getName());
                            db.collection(Constants.COLLECTION_OFFICES).document(officeId)
                                    .collection(Constants.COLLECTION_SERVICES).document(s.getServiceId()).set(s);
                        }
                    }

                    if (!servicesList.isEmpty()) {
                        setupDropdown();
                    } else {
                        Toast.makeText(this, "No services found for this office.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.pbManageSlots.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load services: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                serviceNames
        );
        binding.actvServicePicker.setAdapter(adapter);

        // Default to first service
        selectService(servicesList.get(0));
        binding.actvServicePicker.setText(serviceNames.get(0), false);

        binding.actvServicePicker.setOnItemClickListener((parent, view, position, id) -> {
            selectService(servicesList.get(position));
        });
    }

    private void selectService(OfficeService service) {
        this.selectedService = service;

        binding.switchServiceActive.setChecked(service.isActive());
        binding.etDailyCapacity.setText(String.valueOf(service.getDailyCapacity()));
        binding.etAvgMinutes.setText(String.valueOf(service.getAverageServiceMinutes()));

        String mode = service.getBookingMode();
        if ("INSTANT_ONLY".equalsIgnoreCase(mode)) {
            binding.rbModeInstantOnly.setChecked(true);
        } else if ("APPOINTMENT_ONLY".equalsIgnoreCase(mode)) {
            binding.rbModeAppointmentOnly.setChecked(true);
        } else {
            binding.rbModeBoth.setChecked(true);
        }
    }

    private void saveConfiguration() {
        if (selectedService == null || officeId == null) {
            Toast.makeText(this, "No service selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        String capacityStr = binding.etDailyCapacity.getText() != null ? binding.etDailyCapacity.getText().toString().trim() : "";
        String minutesStr = binding.etAvgMinutes.getText() != null ? binding.etAvgMinutes.getText().toString().trim() : "";

        if (TextUtils.isEmpty(capacityStr)) {
            binding.tilDailyCapacity.setError("Please enter daily capacity");
            return;
        }
        binding.tilDailyCapacity.setError(null);

        if (TextUtils.isEmpty(minutesStr)) {
            binding.tilAvgMinutes.setError("Please enter average duration");
            return;
        }
        binding.tilAvgMinutes.setError(null);

        int capacity;
        int avgMinutes;
        try {
            capacity = Integer.parseInt(capacityStr);
            avgMinutes = Integer.parseInt(minutesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capacity <= 0) {
            binding.tilDailyCapacity.setError("Daily capacity must be greater than 0");
            return;
        }

        if (avgMinutes <= 0 || avgMinutes > 180) {
            binding.tilAvgMinutes.setError("Duration must be between 1 and 180 minutes");
            return;
        }

        boolean isActive = binding.switchServiceActive.isChecked();

        String chosenMode = "BOTH";
        if (binding.rbModeInstantOnly.isChecked()) {
            chosenMode = "INSTANT_ONLY";
        } else if (binding.rbModeAppointmentOnly.isChecked()) {
            chosenMode = "APPOINTMENT_ONLY";
        }

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("active", isActive);
        updates.put("dailyCapacity", capacity);
        updates.put("averageServiceMinutes", avgMinutes);
        updates.put("bookingMode", chosenMode);

        final int finalAvgMinutes = avgMinutes;
        final String finalMode = chosenMode;
        final boolean finalActive = isActive;
        final int finalCapacity = capacity;

        db.collection(Constants.COLLECTION_OFFICES)
                .document(officeId)
                .collection(Constants.COLLECTION_SERVICES)
                .document(selectedService.getServiceId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local object
                    selectedService.setActive(finalActive);
                    selectedService.setDailyCapacity(finalCapacity);
                    selectedService.setAverageServiceMinutes(finalAvgMinutes);
                    selectedService.setBookingMode(finalMode);

                    // Update office-level average duration as well
                    db.collection(Constants.COLLECTION_OFFICES)
                            .document(officeId)
                            .update("averageServiceMinutes", finalAvgMinutes);

                    setLoading(false);
                    Toast.makeText(ManageSlotsActivity.this, "Service settings successfully updated in Firebase!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(ManageSlotsActivity.this, "Error saving settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        binding.pbManageSlots.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSaveSlotConfig.setEnabled(!isLoading);
    }
}
