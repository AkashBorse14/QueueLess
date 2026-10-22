package com.queueless.app.models;

import java.io.Serializable;

/**
 * Represents a Service provided by an Office.
 * Stored under collection: offices/{officeId}/services/{serviceId}
 */
public class OfficeService implements Serializable {
    private String serviceId;
    private String officeId;
    private String name;
    private String description;
    private boolean active = true;
    private int dailyCapacity = 50;
    private int averageServiceMinutes = 5;
    private String bookingMode = "BOTH";

    public OfficeService() {
    }

    public OfficeService(String serviceId, String officeId, String name, String description, boolean active, int dailyCapacity, int averageServiceMinutes, String bookingMode) {
        this.serviceId = serviceId;
        this.officeId = officeId;
        this.name = name;
        this.description = description;
        this.active = active;
        this.dailyCapacity = dailyCapacity;
        this.averageServiceMinutes = averageServiceMinutes;
        this.bookingMode = bookingMode;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getDailyCapacity() {
        return dailyCapacity;
    }

    public void setDailyCapacity(int dailyCapacity) {
        this.dailyCapacity = dailyCapacity;
    }

    public int getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public void setAverageServiceMinutes(int averageServiceMinutes) {
        this.averageServiceMinutes = averageServiceMinutes;
    }

    public String getBookingMode() {
        return bookingMode;
    }

    public void setBookingMode(String bookingMode) {
        this.bookingMode = bookingMode;
    }
}
