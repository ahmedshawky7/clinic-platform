package com.clinic.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "doctor_profiles")
public class DoctorProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long organizationId;

    private Long specialtyId;
    private String specialtyName;
    private String fullName;
    private BigDecimal consultationFee = BigDecimal.valueOf(200); 
    private BigDecimal followUpFee = BigDecimal.valueOf(100); 
    private int avgConsultationMinutes = 10;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(Long specialtyId) { this.specialtyId = specialtyId; }
    public String getSpecialtyName() { return specialtyName; }
    public void setSpecialtyName(String specialtyName) { this.specialtyName = specialtyName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
    public BigDecimal getFollowUpFee() { return followUpFee; }
    public void setFollowUpFee(BigDecimal followUpFee) { this.followUpFee = followUpFee; }
    public int getAvgConsultationMinutes() { return avgConsultationMinutes; }
    public void setAvgConsultationMinutes(int avgConsultationMinutes) { this.avgConsultationMinutes = avgConsultationMinutes; }
}