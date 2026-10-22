package com.queueless.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.queueless.app.databinding.ItemQueueTokenBinding;
import com.queueless.app.models.Appointment;

import java.util.ArrayList;
import java.util.List;

public class AdminQueueAdapter extends RecyclerView.Adapter<AdminQueueAdapter.QueueViewHolder> {

    public interface OnTokenClickListener {
        void onTokenClick(Appointment appointment);
    }

    private List<Appointment> waitingList = new ArrayList<>();
    private final OnTokenClickListener listener;

    public AdminQueueAdapter(OnTokenClickListener listener) {
        this.listener = listener;
    }

    public void setWaitingTokens(List<Appointment> tokens) {
        this.waitingList = tokens != null ? tokens : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQueueTokenBinding binding = ItemQueueTokenBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new QueueViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        holder.bind(waitingList.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return waitingList.size();
    }

    class QueueViewHolder extends RecyclerView.ViewHolder {
        private final ItemQueueTokenBinding binding;

        public QueueViewHolder(@NonNull ItemQueueTokenBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Appointment appointment, int positionInLine) {
            binding.tvAdminTokenBadge.setText(appointment.getTokenNumber());
            binding.tvAdminCustomerName.setText(
                    appointment.getCustomerName() != null ? appointment.getCustomerName() : "Customer"
            );
            binding.tvAdminServiceName.setText(appointment.getServiceName());
            binding.tvAdminQueuePosition.setText("Next in Line: #" + positionInLine);
            binding.tvAdminStatusLabel.setText(appointment.getStatus());

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTokenClick(appointment);
                }
            });
        }
    }
}
