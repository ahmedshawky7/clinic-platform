package com.clinic.application.service;

import com.clinic.application.port.in.GetLiveQueueUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.QueueSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class GetLiveQueueService implements GetLiveQueueUseCase {

    private final AppointmentRepositoryPort repository;
    private final ClinicStatusPort clinicStatusPort;

    public GetLiveQueueService(AppointmentRepositoryPort repository, ClinicStatusPort clinicStatusPort) {
        this.repository = repository;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public QueueSnapshot getSnapshot(Long clinicId, Long doctorId) {
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);

        var current = repository.findCurrentInProgress(clinicId, doctorId, today);
        int waitingCount = repository.countWaiting(clinicId, doctorId, today);
        long estimatedMinutes = clinicStatusPort.calculateEstimatedWaitMinutes(
                clinicId, doctorId, waitingCount, current.isPresent());
        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(clinicId);

        return new QueueSnapshot(
                clinicId,
                doctorId,
                current.map(Appointment::getQueueNumber).orElse(null),
                waitingCount,
                estimatedMinutes,
                arrivalTime,
                LocalDateTime.now(zone));
    }
}