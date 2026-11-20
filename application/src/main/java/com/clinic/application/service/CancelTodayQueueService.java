package com.clinic.application.service;

import com.clinic.application.port.in.CancelTodayQueueUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.QueueEventPublisherPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.QueueSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CancelTodayQueueService implements CancelTodayQueueUseCase {

    private final AppointmentRepositoryPort appointmentRepo;
    private final ClinicStatusPort clinicStatusPort;
    private final QueueEventPublisherPort eventPublisher;

    public CancelTodayQueueService(AppointmentRepositoryPort appointmentRepo,
                                   ClinicStatusPort clinicStatusPort,
                                   QueueEventPublisherPort eventPublisher) {
        this.appointmentRepo = appointmentRepo;
        this.clinicStatusPort = clinicStatusPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CancelResult cancelTodayQueue(Long clinicId, Long doctorId, String reason, boolean closeClinic) {
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate today = now.toLocalDate();

        List<Appointment> activeList = (doctorId != null)
                ? appointmentRepo.findActiveByClinicAndDoctorAndDate(clinicId, doctorId, today)
                : appointmentRepo.findActiveByClinicAndDate(clinicId, today);

        Set<Long> affectedDoctors = new HashSet<>();
        if (doctorId != null) {
            affectedDoctors.add(doctorId);
        }

        for (Appointment appt : activeList) {
            appt.cancel();
            appointmentRepo.save(appt);
            affectedDoctors.add(appt.getDoctorId());
        }

        if (closeClinic) {
            String effectiveReason = (reason != null && !reason.isBlank())
                    ? reason.trim()
                    : "إغلاق طارئ وإلغاء مواعيد اليوم";
            clinicStatusPort.closeClinicToday(clinicId, effectiveReason);
        }

        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(clinicId);
        for (Long docId : affectedDoctors) {
            int remaining = appointmentRepo.countWaiting(clinicId, docId, today);
            var current = appointmentRepo.findCurrentInProgress(clinicId, docId, today);
            eventPublisher.publishQueueUpdate(new QueueSnapshot(
                    clinicId, docId,
                    current.map(Appointment::getQueueNumber).orElse(null),
                    remaining, 0L, arrivalTime, now));
        }

        return new CancelResult(activeList.size(), closeClinic);
    }
}