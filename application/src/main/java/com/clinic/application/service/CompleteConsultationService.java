package com.clinic.application.service;

import com.clinic.application.port.in.CompleteConsultationUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.PrescriptionRepositoryPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.Prescription;

public class CompleteConsultationService implements CompleteConsultationUseCase {

    private final AppointmentRepositoryPort appointmentRepo;
    private final PrescriptionRepositoryPort prescriptionRepo;

    public CompleteConsultationService(AppointmentRepositoryPort appointmentRepo,
                                       PrescriptionRepositoryPort prescriptionRepo) {
        this.appointmentRepo = appointmentRepo;
        this.prescriptionRepo = prescriptionRepo;
    }

    @Override
    public void complete(Long appointmentId, String diagnosis, String medications) {
        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        Prescription prescription = Prescription.create(
                appointment.getOrganizationId(),
                appointment.getClinicId(),
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                diagnosis,
                medications
        );
        prescriptionRepo.save(prescription);

        appointment.complete();
        appointmentRepo.save(appointment);
    }
}