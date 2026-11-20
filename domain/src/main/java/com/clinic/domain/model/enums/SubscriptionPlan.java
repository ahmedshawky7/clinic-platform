package com.clinic.domain.model.enums;

public enum SubscriptionPlan {
    TRIAL(1, 2, 100),
    BASIC(2, 3, 500),
    PRO(5, 15, 3000),
    ENTERPRISE(50, 200, 50000);

    private final int maxClinics;
    private final int maxDoctors;
    private final int maxMonthlyAppointments;

    SubscriptionPlan(int maxClinics, int maxDoctors, int maxMonthlyAppointments) {
        this.maxClinics = maxClinics;
        this.maxDoctors = maxDoctors;
        this.maxMonthlyAppointments = maxMonthlyAppointments;
    }

    public int getMaxClinics() {
        return maxClinics;
    }

    public int getMaxDoctors() {
        return maxDoctors;
    }

    public int getMaxMonthlyAppointments() {
        return maxMonthlyAppointments;
    }
}