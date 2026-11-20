package com.clinic.domain.model;

import java.time.LocalDateTime;

public class Prescription {
    private final Long id;
    private final Long organizationId;
    private final Long clinicId;
    private final Long appointmentId;
    private final Long patientId;
    private final Long doctorId;
    private final String diagnosis;
    private final String medications;
    private final LocalDateTime createdAt;

    public Prescription(Long id, Long organizationId, Long clinicId, Long appointmentId,
                        Long patientId, Long doctorId, String diagnosis, String medications,
                        LocalDateTime createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.clinicId = clinicId;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.medications = medications;
        this.createdAt = createdAt;
    }

    public static Prescription create(Long organizationId, Long clinicId, Long appointmentId,
                                      Long patientId, Long doctorId, String diagnosis, String medications) {
        return new Prescription(null, organizationId, clinicId, appointmentId,
                patientId, doctorId, diagnosis, medications, LocalDateTime.now());
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Long getClinicId() { return clinicId; }
    public Long getAppointmentId() { return appointmentId; }
    public Long getPatientId() { return patientId; }
    public Long getDoctorId() { return doctorId; }
    public String getDiagnosis() { return diagnosis; }
    public String getMedications() { return medications; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}