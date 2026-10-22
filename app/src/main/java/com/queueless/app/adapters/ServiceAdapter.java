package com.queueless.app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.queueless.app.databinding.ItemServiceBinding;
import com.queueless.app.models.OfficeService;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    public interface OnServiceClickListener {
        void onServiceClick(OfficeService service);
    }

    private List<OfficeService> serviceList = new ArrayList<>();
    private final OnServiceClickListener listener;

    public ServiceAdapter(OnServiceClickListener listener) {
        this.listener = listener;
    }

    public void setServices(List<OfficeService> services) {
        this.serviceList = services != null ? services : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServiceBinding binding = ItemServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ServiceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(serviceList.get(position));
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemServiceBinding binding;

        public ServiceViewHolder(@NonNull ItemServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OfficeService service) {
            if (service == null) return;

            binding.tvServiceName.setText(service.getName() != null ? service.getName() : "Service");
            binding.tvServiceDescription.setText(service.getDescription() != null ? service.getDescription() : "");
            binding.tvAvgDuration.setText("⏱ Avg Service: " + service.getAverageServiceMinutes() + " mins");

            // Format booking mode badge
            String mode = service.getBookingMode();
            if ("INSTANT_ONLY".equalsIgnoreCase(mode)) {
                binding.tvBookingModeBadge.setText("INSTANT QUEUE");
            } else if ("APPOINTMENT_ONLY".equalsIgnoreCase(mode)) {
                binding.tvBookingModeBadge.setText("APPOINTMENT ONLY");
            } else {
                binding.tvBookingModeBadge.setText("WALK-IN & APPOINTMENT");
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onServiceClick(service);
                }
            });

            binding.btnSelectService.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onServiceClick(service);
                }
            });
        }
    }
}
