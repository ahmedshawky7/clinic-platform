package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.BookingSource;
import com.clinic.domain.model.enums.VisitType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_clinic_doctor_date_queue",
           columnNames = {"clinic_id", "doctor_id", "scheduled_date", "queue_number"}),
       indexes = {
           @Index(name = "idx_clinic_doctor_date_status", columnList = "clinic_id, doctor_id, scheduled_date, status"),
           @Index(name = "idx_org_date", columnList = "organization_id, scheduled_date")
       })
public class AppointmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "queue_number")
    private Integer queueNumber;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", nullable = false, length = 20)
    private BookingSource bookingSource = BookingSource.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false, columnDefinition = "varchar(20) default 'EXAMINATION'")
    private VisitType visitType = VisitType.EXAMINATION;

    @Column(name = "fee", precision = 10, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "priority", nullable = false, columnDefinition = "boolean default false")
    private boolean priority = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AppointmentEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long v) { this.organizationId = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long v) { this.doctorId = v; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long v) { this.clinicId = v; }
    public Integer getQueueNumber() { return queueNumber; }
    public void setQueueNumber(Integer v) { this.queueNumber = v; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime v) { this.scheduledAt = v; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate v) { this.scheduledDate = v; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus v) { this.status = v; }
    public BookingSource getBookingSource() { return bookingSource; }
    public void setBookingSource(BookingSource v) { this.bookingSource = v; }
    public VisitType getVisitType() { return visitType; }
    public void setVisitType(VisitType v) { this.visitType = v; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal v) { this.fee = v; }
    public boolean isPriority() { return priority; }
    public void setPriority(boolean v) { this.priority = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}