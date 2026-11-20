package com.clinic.domain.exception;

import com.clinic.domain.model.enums.AppointmentStatus;

public class InvalidAppointmentTransitionException extends RuntimeException {

    private final AppointmentStatus currentStatus;
    private final String attemptedAction;

    public InvalidAppointmentTransitionException(AppointmentStatus currentStatus, String attemptedAction) {
        super("Cannot perform '%s' when appointment status is '%s'"
                .formatted(attemptedAction, currentStatus));
        this.currentStatus = currentStatus;
        this.attemptedAction = attemptedAction;
    }

    public AppointmentStatus getCurrentStatus() { return currentStatus; }
    public String getAttemptedAction() { return attemptedAction; }
}