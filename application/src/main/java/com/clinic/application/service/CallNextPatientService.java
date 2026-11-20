package com.clinic.application.service;

import com.clinic.application.port.in.CallNextPatientUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.QueueEventPublisherPort;
import com.clinic.application.result.CallNextResult;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.QueueSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class CallNextPatientService implements CallNextPatientUseCase {

    private final AppointmentRepositoryPort repository;
    private final QueueEventPublisherPort eventPublisher;
    private final ClinicStatusPort clinicStatusPort;

    public CallNextPatientService(AppointmentRepositoryPort repository,
                                  QueueEventPublisherPort eventPublisher,
                                  ClinicStatusPort clinicStatusPort) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public CallNextResult callNext(Long clinicId, Long doctorId) {
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);

        Optional<Appointment> current = repository.findCurrentInProgress(clinicId, doctorId, today);
        Long completedId = current.map(a -> {
            a.complete();
            repository.save(a);
            return a.getId();
        }).orElse(null);

        Optional<Appointment> next = repository.findNextWaitingForUpdate(clinicId, doctorId, today);

        Long startedId = null;
        Integer currentQueueNumber = null;

        if (next.isPresent()) {
            Appointment nextAppt = next.get();
            nextAppt.startConsultation();
            repository.save(nextAppt);
            startedId = nextAppt.getId();
            currentQueueNumber = nextAppt.getQueueNumber();
        }

        int waitingCount = repository.countWaiting(clinicId, doctorId, today);
        boolean consultationStarted = currentQueueNumber != null;
        long estimatedMinutes = clinicStatusPort.calculateEstimatedWaitMinutes(
                clinicId, doctorId, waitingCount, consultationStarted);
        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(clinicId);

        eventPublisher.publishQueueUpdate(new QueueSnapshot(
                clinicId, doctorId, currentQueueNumber,
                waitingCount, estimatedMinutes, arrivalTime, LocalDateTime.now(zone)));

        return new CallNextResult(completedId, startedId, currentQueueNumber);
    }
}