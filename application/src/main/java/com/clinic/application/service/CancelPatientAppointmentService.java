package com.clinic.application.service;

import com.clinic.application.port.in.CancelPatientAppointmentUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.QueueEventPublisherPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.QueueSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CancelPatientAppointmentService implements CancelPatientAppointmentUseCase {

    private final AppointmentRepositoryPort appointmentRepo;
    private final ClinicStatusPort clinicStatusPort;
    private final QueueEventPublisherPort eventPublisher;

    public CancelPatientAppointmentService(AppointmentRepositoryPort appointmentRepo,
                                           ClinicStatusPort clinicStatusPort,
                                           QueueEventPublisherPort eventPublisher) {
        this.appointmentRepo = appointmentRepo;
        this.clinicStatusPort = clinicStatusPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void cancel(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("الحجز غير موجود"));

        if (!appointment.getPatientId().equals(patientId)) {
            throw new SecurityException("غير مصرح لك بإلغاء حجز لا يخصك.");
        }

        appointment.cancelByPatient();
        appointmentRepo.save(appointment);

        ZoneId zone = clinicStatusPort.getClinicZoneId(appointment.getClinicId());
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate today = now.toLocalDate();

        int waitingCount = appointmentRepo.countWaiting(appointment.getClinicId(), appointment.getDoctorId(), today);
        var current = appointmentRepo.findCurrentInProgress(appointment.getClinicId(), appointment.getDoctorId(), today);
        long estimatedMinutes = clinicStatusPort.calculateEstimatedWaitMinutes(
                appointment.getClinicId(), appointment.getDoctorId(), waitingCount, current.isPresent());
        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(appointment.getClinicId());

        eventPublisher.publishQueueUpdate(new QueueSnapshot(
                appointment.getClinicId(),
                appointment.getDoctorId(),
                current.map(Appointment::getQueueNumber).orElse(null),
                waitingCount,
                estimatedMinutes,
                arrivalTime,
                now));
    }
}