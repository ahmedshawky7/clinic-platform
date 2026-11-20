package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.domain.model.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentPersistenceMapper {

    public AppointmentEntity toEntity(Appointment domain) {
        AppointmentEntity e = new AppointmentEntity();
        e.setId(domain.getId());
        e.setOrganizationId(domain.getOrganizationId());
        e.setPatientId(domain.getPatientId());
        e.setDoctorId(domain.getDoctorId());
        e.setClinicId(domain.getClinicId());
        e.setQueueNumber(domain.getQueueNumber());
        e.setScheduledAt(domain.getScheduledAt());
        e.setScheduledDate(domain.getScheduledAt().toLocalDate());
        e.setStatus(domain.getStatus());
        e.setBookingSource(domain.getBookingSource());
        e.setVisitType(domain.getVisitType());
        e.setFee(domain.getFee());
        e.setPriority(domain.isPriority());
        e.setCreatedAt(domain.getCreatedAt());
        return e;
    }

    public Appointment toDomain(AppointmentEntity e) {
        return Appointment.reconstruct(
                e.getId(), e.getOrganizationId(), e.getPatientId(), e.getDoctorId(),
                e.getClinicId(), e.getQueueNumber(), e.getScheduledAt(),
                e.getStatus(), e.getBookingSource(), e.getCreatedAt(),
                e.getVisitType(), e.getFee(), e.isPriority());
    }
}