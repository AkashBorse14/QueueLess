package com.queueless.app.models;

import java.io.Serializable;

/**
 * Represents a registered Office/Department in Cloud Firestore.
 * Stored under collection: offices/{officeId}
 */
public class Office implements Serializable {
    private String officeId;
    private String name;
    private String officeCode;
    private String category;
    private String description;
    private boolean active;
    private int averageServiceMinutes;

    // Required empty constructor for Firestore
    public Office() {
    }

    public Office(String officeId, String name, String officeCode, String category, String description, boolean active, int averageServiceMinutes) {
        this.officeId = officeId;
        this.name = name;
        this.officeCode = officeCode;
        this.category = category;
        this.description = description;
        this.active = active;
        this.averageServiceMinutes = averageServiceMinutes;
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

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public int getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public void setAverageServiceMinutes(int averageServiceMinutes) {
        this.averageServiceMinutes = averageServiceMinutes;
    }
}
