package com.clinic.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "doctor_clinic_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"clinicId", "doctorId"}))
public class DoctorClinicAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long clinicId;

    @Column(nullable = false)
    private Long doctorId;

    private String workingDays;
    private String arrivalTime = "17:00";
    private String departureTime = "22:00";
    private BigDecimal examinationFee; 
    private BigDecimal followUpFee;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getWorkingDays() { return workingDays; }
    public void setWorkingDays(String workingDays) { this.workingDays = workingDays; }
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public BigDecimal getExaminationFee() { return examinationFee; }
    public void setExaminationFee(BigDecimal examinationFee) { this.examinationFee = examinationFee; }
    public BigDecimal getFollowUpFee() { return followUpFee; }
    public void setFollowUpFee(BigDecimal followUpFee) { this.followUpFee = followUpFee; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}