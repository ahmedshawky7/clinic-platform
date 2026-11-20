package com.clinic.application.service;

import com.clinic.application.port.in.GetSkippedPatientsUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.UserQueryPort;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class GetSkippedPatientsService implements GetSkippedPatientsUseCase {
    private final AppointmentRepositoryPort appointmentRepo;
    private final UserQueryPort userQueryPort;
    private final ClinicStatusPort clinicStatusPort;

    public GetSkippedPatientsService(AppointmentRepositoryPort appointmentRepo,
                                     UserQueryPort userQueryPort,
                                     ClinicStatusPort clinicStatusPort) {
        this.appointmentRepo = appointmentRepo;
        this.userQueryPort = userQueryPort;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public List<SkippedPatientDto> getSkippedPatients(Long clinicId, Long doctorId) {
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);
        return appointmentRepo.findSkipped(clinicId, doctorId, today).stream()
                .map(a -> new SkippedPatientDto(
                        a.getId(),
                        a.getQueueNumber() != null ? a.getQueueNumber() : 0,
                        userQueryPort.getPatientNameById(a.getPatientId())))
                .toList();
    }
}