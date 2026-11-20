package com.clinic.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_patients",
       indexes = {
           @Index(name = "idx_blocked_clinic_phone", columnList = "clinic_id, phone_number, active"),
           @Index(name = "idx_blocked_clinic_patient", columnList = "clinic_id, patient_id, active")
       })
public class BlockedPatientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    private String patientName;
    private String reason;

    @Column(name = "blocked_by_user_id", nullable = false)
    private Long blockedByUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getBlockedByUserId() { return blockedByUserId; }
    public void setBlockedByUserId(Long blockedByUserId) { this.blockedByUserId = blockedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}