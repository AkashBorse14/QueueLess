package com.queueless.app.utils;

public class Constants {
    // Roles
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN = "ADMIN";

    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_OFFICES = "offices";
    public static final String COLLECTION_SERVICES = "services";
    public static final String COLLECTION_APPOINTMENTS = "appointments";
    public static final String COLLECTION_QUEUE_COUNTERS = "queueCounters";

    // Booking & Token Statuses
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_SERVING = "SERVING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Booking Modes
    public static final String MODE_INSTANT = "INSTANT";
    public static final String MODE_SCHEDULED = "SCHEDULED";
}
