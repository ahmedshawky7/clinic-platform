package com.clinic.application.service;

import com.clinic.application.port.in.AddWalkInPatientUseCase;
import com.clinic.application.port.out.*;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.Organization;
import com.clinic.domain.model.QueueSnapshot;
import com.clinic.domain.model.enums.BookingSource;
import com.clinic.domain.model.enums.VisitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class AddWalkInPatientService implements AddWalkInPatientUseCase {

    private final UserRegistrationPort userRegistrationPort;
    private final PasswordHasherPort passwordHasherPort;
    private final AppointmentRepositoryPort appointmentRepo;
    private final QueueEventPublisherPort eventPublisher;
    private final ClinicStatusPort clinicStatusPort;
    private final OrganizationRepositoryPort organizationRepo;

    public AddWalkInPatientService(UserRegistrationPort userRegistrationPort,
                                   PasswordHasherPort passwordHasherPort,
                                   AppointmentRepositoryPort appointmentRepo,
                                   QueueEventPublisherPort eventPublisher,
                                   ClinicStatusPort clinicStatusPort,
                                   OrganizationRepositoryPort organizationRepo) {
        this.userRegistrationPort = userRegistrationPort;
        this.passwordHasherPort = passwordHasherPort;
        this.appointmentRepo = appointmentRepo;
        this.eventPublisher = eventPublisher;
        this.clinicStatusPort = clinicStatusPort;
        this.organizationRepo = organizationRepo;
    }

    @Override
    public void addWalkIn(Long patientId, String fullName, LocalDate dateOfBirth,
                          String phoneNumber, Long clinicId, Long doctorId,
                          VisitType visitType, boolean priority) {
        if (!clinicStatusPort.isDoctorAssignedToClinic(clinicId, doctorId)) {
            throw new IllegalArgumentException("الطبيب غير مرتبط بهذا الفرع.");
        }

        Long orgId = clinicStatusPort.getOrganizationIdByClinicId(clinicId);
        Organization org = organizationRepo.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));

        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate today = now.toLocalDate();

        long monthlyCount = appointmentRepo.countByOrganizationInMonth(orgId, today.getYear(), today.getMonthValue());
        org.ensureCanBookThisMonth(monthlyCount);

        if (patientId != null) {
            var existing = appointmentRepo.findActiveByPatientInClinic(patientId, clinicId, today);
            if (existing.isPresent()) {
                throw new IllegalStateException(
                        "هذا المريض لديه حجز نشط بالفعل في العيادة اليوم برقم: " + existing.get().getQueueNumber()
                );
            }
        }

        Long finalPatientId = patientId;
        if (finalPatientId == null) {
            String uniqueUsername = "walkin_" + UUID.randomUUID().toString().substring(0, 8);
            String dummyPassword = passwordHasherPort.hash(UUID.randomUUID().toString());
            finalPatientId = userRegistrationPort.savePatient(
                    uniqueUsername, dummyPassword, fullName, dateOfBirth, phoneNumber, orgId);
        }

        VisitType resolvedType = visitType != null ? visitType : VisitType.EXAMINATION;
        BigDecimal fee = clinicStatusPort.resolveVisitFee(clinicId, doctorId, resolvedType);
        int queueNumber = appointmentRepo.nextQueueNumber(clinicId, doctorId, today);

        Appointment appointment = Appointment.book(
                orgId, finalPatientId, doctorId, clinicId, queueNumber, now,
                BookingSource.WALK_IN, now, resolvedType, fee, priority);
        appointmentRepo.save(appointment);

        int waitingCount = appointmentRepo.countWaiting(clinicId, doctorId, today);
        var current = appointmentRepo.findCurrentInProgress(clinicId, doctorId, today);
        long estimatedMinutes = clinicStatusPort.calculateEstimatedWaitMinutes(
                clinicId, doctorId, waitingCount, current.isPresent());
        String arrivalTime = clinicStatusPort.getDoctorArrivalTime(clinicId);

        eventPublisher.publishQueueUpdate(new QueueSnapshot(
                clinicId, doctorId,
                current.map(Appointment::getQueueNumber).orElse(null),
                waitingCount, estimatedMinutes, arrivalTime, now));
    }
}