package com.clinic.application.service;

import com.clinic.application.port.in.CallNextPatientUseCase;
import com.clinic.application.port.in.SkipPatientUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.domain.model.Appointment;

import java.time.LocalDate;
import java.time.ZoneId;

public class SkipPatientService implements SkipPatientUseCase {
    private final AppointmentRepositoryPort repository;
    private final CallNextPatientUseCase callNextUseCase;
    private final ClinicStatusPort clinicStatusPort;

    public SkipPatientService(AppointmentRepositoryPort repository,
                              CallNextPatientUseCase callNextUseCase,
                              ClinicStatusPort clinicStatusPort) {
        this.repository = repository;
        this.callNextUseCase = callNextUseCase;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public void skipCurrent(Long clinicId, Long doctorId) {
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);
        var current = repository.findCurrentInProgress(clinicId, doctorId, today);
        if (current.isPresent()) {
            Appointment appointment = current.get();
            appointment.skip();
            repository.save(appointment);
            callNextUseCase.callNext(clinicId, doctorId);
        }
    }
}