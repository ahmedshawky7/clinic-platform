package com.clinic.domain.model;

import com.clinic.domain.exception.InvalidAppointmentTransitionException;
import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.BookingSource;
import com.clinic.domain.model.enums.VisitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Appointment {

    private final Long id;
    private final Long organizationId;
    private final Long patientId;
    private final Long doctorId;
    private final Long clinicId;
    private final Integer queueNumber;
    private final LocalDateTime scheduledAt;
    private final LocalDateTime createdAt;
    private final BookingSource bookingSource;
    private final VisitType visitType;
    private final BigDecimal fee;
    private final boolean priority;
    private AppointmentStatus status;

    private Appointment(Long id, Long organizationId, Long patientId, Long doctorId, Long clinicId,
                        Integer queueNumber, LocalDateTime scheduledAt, AppointmentStatus status,
                        BookingSource bookingSource, LocalDateTime createdAt,
                        VisitType visitType, BigDecimal fee, boolean priority) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.doctorId = Objects.requireNonNull(doctorId, "doctorId");
        this.clinicId = Objects.requireNonNull(clinicId, "clinicId");
        this.queueNumber = queueNumber;
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt");
        this.status = Objects.requireNonNull(status, "status");
        this.bookingSource = Objects.requireNonNull(bookingSource, "bookingSource");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.visitType = visitType != null ? visitType : VisitType.EXAMINATION;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.priority = priority;
    }

    public static Appointment book(Long organizationId, Long patientId, Long doctorId, Long clinicId,
                                   int queueNumber, LocalDateTime scheduledAt, BookingSource source) {
        return book(organizationId, patientId, doctorId, clinicId, queueNumber, scheduledAt, source, LocalDateTime.now());
    }

    public static Appointment book(Long organizationId, Long patientId, Long doctorId, Long clinicId,
                                   int queueNumber, LocalDateTime scheduledAt, BookingSource source, LocalDateTime nowInClinicZone) {
        return book(organizationId, patientId, doctorId, clinicId, queueNumber, scheduledAt, source,
                nowInClinicZone, VisitType.EXAMINATION, BigDecimal.ZERO, false);
    }

    public static Appointment book(Long organizationId, Long patientId, Long doctorId, Long clinicId,
                                   int queueNumber, LocalDateTime scheduledAt, BookingSource source, LocalDateTime nowInClinicZone,
                                   VisitType visitType, BigDecimal fee, boolean priority) {
        if (queueNumber <= 0) {
            throw new IllegalArgumentException("queueNumber must be positive");
        }
        if (source == BookingSource.ONLINE && scheduledAt.isBefore(nowInClinicZone.minusMinutes(5))) {
            throw new IllegalArgumentException("لا يمكن الحجز في وقت ماضٍ");
        }
        return new Appointment(null, organizationId, patientId, doctorId, clinicId,
                queueNumber, scheduledAt, AppointmentStatus.WAITING, source, nowInClinicZone,
                visitType, fee, priority);
    }

    public static Appointment createDirectVisit(Long organizationId, Long patientId, Long doctorId,
                                                Long clinicId, LocalDateTime nowInClinicZone,
                                                VisitType visitType, BigDecimal fee) {
        return new Appointment(
                null, organizationId, patientId, doctorId, clinicId,
                null,
                nowInClinicZone,
                AppointmentStatus.COMPLETED,
                BookingSource.DIRECT_VISIT,
                nowInClinicZone,
                visitType,
                fee,
                false);
    }

    public static Appointment createDirectVisit(Long organizationId, Long patientId, Long doctorId,
                                                Long clinicId, LocalDateTime nowInClinicZone) {
        return createDirectVisit(organizationId, patientId, doctorId, clinicId, nowInClinicZone,
                VisitType.EXAMINATION, BigDecimal.ZERO);
    }

    public static Appointment createDirectVisit(Long organizationId, Long patientId, Long doctorId, Long clinicId) {
        return createDirectVisit(organizationId, patientId, doctorId, clinicId, LocalDateTime.now());
    }

    public static Appointment reconstruct(Long id, Long organizationId, Long patientId, Long doctorId, Long clinicId,
                                          Integer queueNumber, LocalDateTime scheduledAt, AppointmentStatus status,
                                          BookingSource bookingSource, LocalDateTime createdAt) {
        return reconstruct(id, organizationId, patientId, doctorId, clinicId, queueNumber, scheduledAt, status,
                bookingSource, createdAt, VisitType.EXAMINATION, BigDecimal.ZERO, false);
    }

    public static Appointment reconstruct(Long id, Long organizationId, Long patientId, Long doctorId, Long clinicId,
                                          Integer queueNumber, LocalDateTime scheduledAt, AppointmentStatus status,
                                          BookingSource bookingSource, LocalDateTime createdAt,
                                          VisitType visitType, BigDecimal fee, boolean priority) {
        return new Appointment(id, organizationId, patientId, doctorId, clinicId,
                queueNumber, scheduledAt, status, bookingSource, createdAt,
                visitType, fee, priority);
    }

    public void startConsultation() {
        if (status != AppointmentStatus.WAITING) {
            throw new InvalidAppointmentTransitionException(status, "startConsultation");
        }
        this.status = AppointmentStatus.IN_PROGRESS;
    }

    public void complete() {
        if (status != AppointmentStatus.IN_PROGRESS) {
            throw new InvalidAppointmentTransitionException(status, "complete");
        }
        this.status = AppointmentStatus.COMPLETED;
    }

    public void cancel() {
        if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED) {
            throw new InvalidAppointmentTransitionException(status, "cancel");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void cancelByPatient() {
        if (status != AppointmentStatus.WAITING && status != AppointmentStatus.SKIPPED) {
            throw new IllegalStateException("لا يمكن إلغاء الحجز بعد بدء الكشف أو انتهائه.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void skip() {
        if (status != AppointmentStatus.WAITING && status != AppointmentStatus.IN_PROGRESS) {
            throw new InvalidAppointmentTransitionException(status, "skip");
        }
        this.status = AppointmentStatus.SKIPPED;
    }

    public void requeue() {
        if (status != AppointmentStatus.SKIPPED) {
            throw new InvalidAppointmentTransitionException(status, "requeue");
        }
        this.status = AppointmentStatus.WAITING;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Long getPatientId() { return patientId; }
    public Long getDoctorId() { return doctorId; }
    public Long getClinicId() { return clinicId; }
    public Integer getQueueNumber() { return queueNumber; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BookingSource getBookingSource() { return bookingSource; }
    public VisitType getVisitType() { return visitType; }
    public BigDecimal getFee() { return fee; }
    public boolean isPriority() { return priority; }
    public AppointmentStatus getStatus() { return status; }
}