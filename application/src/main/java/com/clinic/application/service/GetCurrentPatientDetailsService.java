package com.clinic.application.service;

import com.clinic.application.port.in.GetCurrentPatientDetailsUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.PrescriptionRepositoryPort;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.PatientProfile;

import java.time.LocalDate;
import java.time.ZoneId;

public class GetCurrentPatientDetailsService implements GetCurrentPatientDetailsUseCase {
    private final AppointmentRepositoryPort appointmentRepo;
    private final PrescriptionRepositoryPort prescriptionRepo;
    private final UserQueryPort userQueryPort;
    private final ClinicStatusPort clinicStatusPort;

    public GetCurrentPatientDetailsService(AppointmentRepositoryPort appointmentRepo,
                                           PrescriptionRepositoryPort prescriptionRepo,
                                           UserQueryPort userQueryPort,
                                           ClinicStatusPort clinicStatusPort) {
        this.appointmentRepo = appointmentRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.userQueryPort = userQueryPort;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public PatientDetails getCurrentPatientDetails(Long clinicId, Long doctorId) {
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);
        Appointment current = appointmentRepo.findCurrentInProgress(clinicId, doctorId, today)
                .orElseThrow(() -> new IllegalArgumentException("لا يوجد مريض حالياً"));
        PatientProfile profile = userQueryPort.getPatientProfile(current.getPatientId());
        var history = prescriptionRepo.findByPatientId(current.getPatientId());
        return new PatientDetails(profile.fullName(), current.getId(), profile.dateOfBirth(), history);
    }
}