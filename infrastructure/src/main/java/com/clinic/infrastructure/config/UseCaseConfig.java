package com.clinic.infrastructure.config;

import com.clinic.application.port.in.*;
import com.clinic.application.port.out.*;
import com.clinic.application.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class UseCaseConfig {

    @Bean
    public BookAppointmentUseCase bookAppointmentUseCase(
            AppointmentRepositoryPort repo, QueueEventPublisherPort publisher,
            ClinicStatusPort clinicStatusPort, OrganizationRepositoryPort orgRepo,
            BlockedPatientPort blockedPatientPort, TransactionTemplate tx) {
        var core = new BookAppointmentService(repo, publisher, clinicStatusPort, orgRepo, blockedPatientPort);
        return new TransactionalBookAppointmentUseCase(core, tx);
    }

    @Bean
    public CallNextPatientUseCase callNextPatientUseCase(
            AppointmentRepositoryPort repo, QueueEventPublisherPort publisher,
            ClinicStatusPort clinicStatusPort, TransactionTemplate tx) {
        var core = new CallNextPatientService(repo, publisher, clinicStatusPort);
        return new TransactionalCallNextPatientUseCase(core, tx);
    }

    @Bean
    public CompleteConsultationUseCase completeConsultationUseCase(
            AppointmentRepositoryPort appointmentRepo, PrescriptionRepositoryPort prescriptionRepo, TransactionTemplate tx) {
        var core = new CompleteConsultationService(appointmentRepo, prescriptionRepo);
        return new TransactionalCompleteConsultationUseCase(core, tx);
    }

    @Bean
    public GetLiveQueueUseCase getLiveQueueUseCase(AppointmentRepositoryPort repo, ClinicStatusPort clinicStatusPort) {
        return new GetLiveQueueService(repo, clinicStatusPort);
    }

    @Bean
    public GetPatientPrescriptionsUseCase getPatientPrescriptionsUseCase(PrescriptionRepositoryPort prescriptionRepo) {
        return new GetPatientPrescriptionsService(prescriptionRepo);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRegistrationPort userRepo, PasswordHasherPort passwordHasher) {
        return new RegisterUserService(userRepo, passwordHasher);
    }

    @Bean
    public GetCurrentPatientDetailsUseCase getCurrentPatientDetailsUseCase(
            AppointmentRepositoryPort appointmentRepo, PrescriptionRepositoryPort prescriptionRepo,
            UserQueryPort userQueryPort, ClinicStatusPort clinicStatusPort) {
        return new GetCurrentPatientDetailsService(appointmentRepo, prescriptionRepo, userQueryPort, clinicStatusPort);
    }

    @Bean
    public SkipPatientUseCase skipPatientUseCase(
            AppointmentRepositoryPort repo, CallNextPatientUseCase callNextUseCase,
            ClinicStatusPort clinicStatusPort, TransactionTemplate tx) {
        var core = new SkipPatientService(repo, callNextUseCase, clinicStatusPort);
        return (clinicId, doctorId) -> tx.executeWithoutResult(s -> core.skipCurrent(clinicId, doctorId));
    }

    @Bean
    public RequeuePatientUseCase requeuePatientUseCase(
            AppointmentRepositoryPort repo, QueueEventPublisherPort publisher,
            ClinicStatusPort clinicStatusPort, TransactionTemplate tx) {
        var core = new RequeuePatientService(repo, publisher, clinicStatusPort);
        return (appointmentId, clinicId, doctorId) -> tx.executeWithoutResult(s -> core.requeue(appointmentId, clinicId, doctorId));
    }

    @Bean
    public GetSkippedPatientsUseCase getSkippedPatientsUseCase(
            AppointmentRepositoryPort repo, UserQueryPort userQueryPort, ClinicStatusPort clinicStatusPort) {
        return new GetSkippedPatientsService(repo, userQueryPort, clinicStatusPort);
    }

    @Bean
    public AddWalkInPatientUseCase addWalkInPatientUseCase(
            UserRegistrationPort userRegistrationPort, PasswordHasherPort passwordHasherPort,
            AppointmentRepositoryPort appointmentRepo, QueueEventPublisherPort eventPublisher,
            ClinicStatusPort clinicStatusPort, OrganizationRepositoryPort orgRepo, TransactionTemplate tx) {
        var core = new AddWalkInPatientService(
                userRegistrationPort, passwordHasherPort, appointmentRepo, eventPublisher, clinicStatusPort, orgRepo);
        return (patientId, fullName, dob, phone, clinicId, doctorId, visitType, priority) ->
                tx.executeWithoutResult(s -> core.addWalkIn(patientId, fullName, dob, phone, clinicId, doctorId, visitType, priority));
    }

    @Bean
    public SearchPatientsUseCase searchPatientsUseCase(UserQueryPort userQueryPort) {
        return new SearchPatientsService(userQueryPort);
    }

    @Bean
    public GetPatientFullFileUseCase getPatientFullFileUseCase(
            UserQueryPort userQueryPort, PrescriptionRepositoryPort prescriptionRepo) {
        return new GetPatientFullFileService(userQueryPort, prescriptionRepo);
    }

    @Bean
    public DirectVisitUseCase directVisitUseCase(
            AppointmentRepositoryPort appointmentRepo, PrescriptionRepositoryPort prescriptionRepo,
            ClinicStatusPort clinicStatusPort, TransactionTemplate tx) {
        var core = new DirectVisitService(appointmentRepo, prescriptionRepo, clinicStatusPort);
        return (patientId, clinicId, doctorId, diagnosis, medications, visitType) ->
                tx.executeWithoutResult(s -> core.addDirectVisit(patientId, clinicId, doctorId, diagnosis, medications, visitType));
    }

    @Bean
    public GeneratePatientAccountUseCase generatePatientAccountUseCase(
            UserRegistrationPort userRepo, UserQueryPort userQueryPort, PasswordHasherPort passwordHasher) {
        return new GeneratePatientAccountService(userRepo, userQueryPort, passwordHasher);
    }

    @Bean
    public CancelTodayQueueUseCase cancelTodayQueueUseCase(
            AppointmentRepositoryPort appointmentRepo, ClinicStatusPort clinicStatusPort,
            QueueEventPublisherPort eventPublisher, TransactionTemplate tx) {
        var core = new CancelTodayQueueService(appointmentRepo, clinicStatusPort, eventPublisher);
        return (clinicId, doctorId, reason, closeClinic) ->
                tx.execute(s -> core.cancelTodayQueue(clinicId, doctorId, reason, closeClinic));
    }

    @Bean
    public CancelPatientAppointmentUseCase cancelPatientAppointmentUseCase(
            AppointmentRepositoryPort appointmentRepo, ClinicStatusPort clinicStatusPort,
            QueueEventPublisherPort eventPublisher, TransactionTemplate tx) {
        var core = new CancelPatientAppointmentService(appointmentRepo, clinicStatusPort, eventPublisher);
        return (appointmentId, patientId) ->
                tx.executeWithoutResult(s -> core.cancel(appointmentId, patientId));
    }
}