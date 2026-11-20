package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.application.port.out.PrescriptionRepositoryPort;
import com.clinic.domain.model.Prescription;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PrescriptionPersistenceAdapter implements PrescriptionRepositoryPort {
    private final PrescriptionJpaRepository jpaRepository;

    public PrescriptionPersistenceAdapter(PrescriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Prescription save(Prescription p) {
        PrescriptionEntity e = new PrescriptionEntity();
        e.setOrganizationId(p.getOrganizationId());
        e.setClinicId(p.getClinicId());
        e.setAppointmentId(p.getAppointmentId());
        e.setPatientId(p.getPatientId());
        e.setDoctorId(p.getDoctorId());
        e.setDiagnosis(p.getDiagnosis());
        e.setMedications(p.getMedications());
        e.setCreatedAt(p.getCreatedAt());
        PrescriptionEntity s = jpaRepository.save(e);
        return new Prescription(s.getId(), s.getOrganizationId(), s.getClinicId(),
                s.getAppointmentId(), s.getPatientId(), s.getDoctorId(),
                s.getDiagnosis(), s.getMedications(), s.getCreatedAt());
    }

    @Override
    public List<Prescription> findByPatientId(Long patientId) {
        return jpaRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(e -> new Prescription(e.getId(), e.getOrganizationId(), e.getClinicId(),
                        e.getAppointmentId(), e.getPatientId(), e.getDoctorId(),
                        e.getDiagnosis(), e.getMedications(), e.getCreatedAt()))
                .toList();
    }
}