package com.queueless.app.models;

import java.io.Serializable;

/**
 * Model representing a registered User in Cloud Firestore.
 * Stored under collection: users/{uid}
 */
public class User implements Serializable {
    private String uid;
    private String name;
    private String email;
    private String role; // "CUSTOMER" or "ADMIN"
    private String officeId; // Assigned only for office admins
    private long createdAt;

    // Required empty constructor for Firestore automatic deserialization
    public User() {
    }

    public User(String uid, String name, String email, String role, String officeId, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.officeId = officeId;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
