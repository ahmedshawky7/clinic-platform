package com.clinic.application.service;

import com.clinic.application.port.in.DirectVisitUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.PrescriptionRepositoryPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.Prescription;
import com.clinic.domain.model.enums.VisitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DirectVisitService implements DirectVisitUseCase {

    private final AppointmentRepositoryPort appointmentRepo;
    private final PrescriptionRepositoryPort prescriptionRepo;
    private final ClinicStatusPort clinicStatusPort;

    public DirectVisitService(AppointmentRepositoryPort appointmentRepo,
                              PrescriptionRepositoryPort prescriptionRepo,
                              ClinicStatusPort clinicStatusPort) {
        this.appointmentRepo = appointmentRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public void addDirectVisit(Long patientId, Long clinicId, Long doctorId,
                               String diagnosis, String medications, VisitType visitType) {
        Long orgId = clinicStatusPort.getOrganizationIdByClinicId(clinicId);
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDateTime nowInClinic = LocalDateTime.now(zone);

        VisitType resolvedType = visitType != null ? visitType : VisitType.EXAMINATION;
        BigDecimal fee = clinicStatusPort.resolveVisitFee(clinicId, doctorId, resolvedType);

        Appointment directVisit = Appointment.createDirectVisit(
                orgId, patientId, doctorId, clinicId, nowInClinic, resolvedType, fee);
        Appointment saved = appointmentRepo.save(directVisit);

        Prescription prescription = Prescription.create(
                orgId, clinicId, saved.getId(), patientId, doctorId, diagnosis, medications
        );
        prescriptionRepo.save(prescription);
    }
}