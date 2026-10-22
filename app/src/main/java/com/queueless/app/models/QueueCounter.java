package com.queueless.app.models;

import java.io.Serializable;

/**
 * Represents a Concurrency-safe atomic counter for an Office on a specific date.
 * Stored under collection: queueCounters/{officeId_YYYY-MM-DD}
 */
public class QueueCounter implements Serializable {
    private String officeId;
    private String queueDate;
    private long lastIssuedNumber;
    private long currentServingSequence;

    public QueueCounter() {
    }

    public QueueCounter(String officeId, String queueDate, long lastIssuedNumber, long currentServingSequence) {
        this.officeId = officeId;
        this.queueDate = queueDate;
        this.lastIssuedNumber = lastIssuedNumber;
        this.currentServingSequence = currentServingSequence;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(String queueDate) {
        this.queueDate = queueDate;
    }

    public long getLastIssuedNumber() {
        return lastIssuedNumber;
    }

    public void setLastIssuedNumber(long lastIssuedNumber) {
        this.lastIssuedNumber = lastIssuedNumber;
    }

    public long getCurrentServingSequence() {
        return currentServingSequence;
    }

    public void setCurrentServingSequence(long currentServingSequence) {
        this.currentServingSequence = currentServingSequence;
    }
}
