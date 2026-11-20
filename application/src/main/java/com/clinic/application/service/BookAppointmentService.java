package com.clinic.application.service;

import com.clinic.application.command.BookAppointmentCommand;
import com.clinic.application.port.in.BookAppointmentUseCase;
import com.clinic.application.port.out.*;
import com.clinic.application.result.AppointmentResult;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.Organization;
import com.clinic.domain.model.QueueSnapshot;
import com.clinic.domain.model.enums.BookingSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class BookAppointmentService implements BookAppointmentUseCase {

    private final AppointmentRepositoryPort repository;
    private final QueueEventPublisherPort eventPublisher;
    private final ClinicStatusPort clinicStatusPort;
    private final OrganizationRepositoryPort organizationRepo;
    private final BlockedPatientPort blockedPatientPort;

    public BookAppointmentService(AppointmentRepositoryPort repository,
                                  QueueEventPublisherPort eventPublisher,
                                  ClinicStatusPort clinicStatusPort,
                                  OrganizationRepositoryPort organizationRepo,
                                  BlockedPatientPort blockedPatientPort) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clinicStatusPort = clinicStatusPort;
        this.organizationRepo = organizationRepo;
        this.blockedPatientPort = blockedPatientPort;
    }

    @Override
    public AppointmentResult book(BookAppointmentCommand command) {
        if (!clinicStatusPort.isClinicOpen(command.clinicId())) {
            throw new IllegalStateException("العيادة مغلقة اليوم. السبب: " + clinicStatusPort.getClosureReason(command.clinicId()));
        }

        if (!clinicStatusPort.isWithinBookingHours(command.clinicId())) {
            throw new IllegalStateException(clinicStatusPort.getBookingClosedMessage(command.clinicId()));
        }

        if (!clinicStatusPort.isDoctorAssignedToClinic(command.clinicId(), command.doctorId())) {
            throw new IllegalArgumentException("الطبيب غير متاح في هذا الفرع.");
        }

        if (blockedPatientPort.isPatientBlockedInClinic(command.clinicId(), command.patientId())) {
            throw new IllegalStateException("عذراً، تم تقييد الحجز الإلكتروني لحسابك في هذه العيادة. يرجى التواصل مع الاستقبال.");
        }

        Long orgId = clinicStatusPort.getOrganizationIdByClinicId(command.clinicId());
        Organization org = organizationRepo.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));

        ZoneId clinicZone = clinicStatusPort.getClinicZoneId(command.clinicId());
        LocalDateTime nowInClinic = LocalDateTime.now(clinicZone);
        LocalDate date = nowInClinic.toLocalDate();

        long monthlyCount = repository.countByOrganizationInMonth(orgId, date.getYear(), date.getMonthValue());
        org.ensureCanBookThisMonth(monthlyCount);

        var existingInClinic = repository.findActiveByPatientInClinic(command.patientId(), command.clinicId(), date);
        if (existingInClinic.isPresent()) {
            Integer currentQ = existingInClinic.get().getQueueNumber();
            throw new IllegalStateException(
                    "لديك حجز نشط بالفعل في هذه العيادة اليوم. رقم دورك الحالي هو: " + (currentQ != null ? currentQ : "-")
            );
        }

        int queueNumber = repository.nextQueueNumber(command.clinicId(), command.doctorId(), date);
        BigDecimal fee = clinicStatusPort.resolveVisitFee(command.clinicId(), command.doctorId(), command.visitType());

        Appointment appointment = Appointment.book(
                orgId, command.patientId(), command.doctorId(), command.clinicId(),
                queueNumber, command.scheduledAt(), BookingSource.ONLINE, nowInClinic,
                command.visitType(), fee, false);

        Appointment saved = repository.save(appointment);

        var current = repository.findCurrentInProgress(saved.getClinicId(), saved.getDoctorId(), date);
        int waitingCount = repository.countWaiting(saved.getClinicId(), saved.getDoctorId(), date);
        long estimatedMinutes = clinicStatusPort.calculateEstimatedWaitMinutes(
                saved.getClinicId(), saved.getDoctorId(), waitingCount, current.isPresent());
        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(saved.getClinicId());

        eventPublisher.publishQueueUpdate(new QueueSnapshot(
                saved.getClinicId(), saved.getDoctorId(),
                current.map(Appointment::getQueueNumber).orElse(null),
                waitingCount, estimatedMinutes, arrivalTime, nowInClinic));

        return AppointmentResult.from(saved);
    }
}