package com.clinic.application.service;

import com.clinic.application.port.in.RequeuePatientUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.QueueEventPublisherPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.QueueSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class RequeuePatientService implements RequeuePatientUseCase {
    private final AppointmentRepositoryPort repository;
    private final QueueEventPublisherPort eventPublisher;
    private final ClinicStatusPort clinicStatusPort;

    public RequeuePatientService(AppointmentRepositoryPort repository,
                                 QueueEventPublisherPort eventPublisher,
                                 ClinicStatusPort clinicStatusPort) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clinicStatusPort = clinicStatusPort;
    }

    @Override
    public void requeue(Long appointmentId, Long clinicId, Long doctorId) {
        Appointment appointment = repository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.getClinicId().equals(clinicId) || !appointment.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("الحجز لا ينتمي لهذا الفرع أو الطبيب");
        }
        appointment.requeue();
        repository.save(appointment);

        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);
        int waitingCount = repository.countWaiting(clinicId, doctorId, today);
        var current = repository.findCurrentInProgress(clinicId, doctorId, today);
        long estimatedMinutes = clinicStatusPort.calculateEstimatedWaitMinutes(
                clinicId, doctorId, waitingCount, current.isPresent());
        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(clinicId);

        eventPublisher.publishQueueUpdate(new QueueSnapshot(
                clinicId, doctorId,
                current.map(Appointment::getQueueNumber).orElse(null),
                waitingCount, estimatedMinutes, arrivalTime, LocalDateTime.now(zone)));
    }
}