package com.queueless.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.queueless.app.R;
import com.queueless.app.databinding.ItemBookingHistoryBinding;
import com.queueless.app.models.Appointment;
import com.queueless.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.HistoryViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(Appointment appointment);
    }

    private List<Appointment> appointmentList = new ArrayList<>();
    private final OnBookingClickListener listener;

    public BookingHistoryAdapter(OnBookingClickListener listener) {
        this.listener = listener;
    }

    public void setAppointments(List<Appointment> list) {
        this.appointmentList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookingHistoryBinding binding = ItemBookingHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(appointmentList.get(position));
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemBookingHistoryBinding binding;

        public HistoryViewHolder(@NonNull ItemBookingHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Appointment appt) {
            Context context = itemView.getContext();

            binding.tvHistoryOfficeName.setText(appt.getOfficeName());
            binding.tvHistoryServiceName.setText(appt.getServiceName());
            binding.tvHistoryTokenNumber.setText("#" + appt.getTokenNumber());

            // Booking type label
            if (Constants.MODE_INSTANT.equalsIgnoreCase(appt.getBookingType())) {
                binding.tvHistoryBookingType.setText("⚡ Walk-In Queue");
            } else {
                binding.tvHistoryBookingType.setText("📅 Appointment");
            }

            // Date / Time display
            String dateTime = appt.getAppointmentDate();
            if (appt.getAppointmentTime() != null && !appt.getAppointmentTime().isEmpty()) {
                dateTime += " • " + appt.getAppointmentTime();
            }
            binding.tvHistoryDateTime.setText(dateTime);

            // Status Styling
            String status = appt.getStatus();
            binding.tvHistoryStatusBadge.setText(status);

            if (Constants.STATUS_SERVING.equalsIgnoreCase(status)) {
                binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_serving));
            } else if (Constants.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_completed));
            } else if (Constants.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled));
            } else if (Constants.STATUS_SKIPPED.equalsIgnoreCase(status)) {
                binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_skipped));
            } else if (Constants.STATUS_BOOKED.equalsIgnoreCase(status)) {
                binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
            } else {
                binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_waiting));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookingClick(appt);
                }
            });
        }
    }
}
