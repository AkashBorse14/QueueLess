package com.queueless.app.models;

import java.io.Serializable;

/**
 * Represents an Appointment or Queue Token in Cloud Firestore.
 * Stored under collection: appointments/{appointmentId}
 */
public class Appointment implements Serializable {
    private String appointmentId;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String officeId;
    private String officeName;
    private String serviceId;
    private String serviceName;
    private String bookingType; // "INSTANT" or "SCHEDULED"
    private String appointmentDate; // YYYY-MM-DD
    private String appointmentTime; // e.g. "10:30 AM" or ""
    private String tokenNumber; // e.g. "A001"
    private long queueSequence; // 1, 2, 3...
    private String status; // BOOKED, WAITING, SERVING, COMPLETED, SKIPPED, CANCELLED
    private long createdAt;
    private long updatedAt;

    public Appointment() {
    }

    public Appointment(String appointmentId, String customerId, String customerName, String customerEmail,
                       String officeId, String officeName, String serviceId, String serviceName,
                       String bookingType, String appointmentDate, String appointmentTime,
                       String tokenNumber, long queueSequence, String status, long createdAt, long updatedAt) {
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.officeId = officeId;
        this.officeName = officeName;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.bookingType = bookingType;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.tokenNumber = tokenNumber;
        this.queueSequence = queueSequence;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public long getQueueSequence() {
        return queueSequence;
    }

    public void setQueueSequence(long queueSequence) {
        this.queueSequence = queueSequence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
