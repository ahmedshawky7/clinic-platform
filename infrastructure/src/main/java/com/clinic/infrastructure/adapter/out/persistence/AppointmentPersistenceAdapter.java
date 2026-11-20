package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.BookingSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class AppointmentPersistenceAdapter implements AppointmentRepositoryPort {

    private final AppointmentJpaRepository jpa;
    private final AppointmentPersistenceMapper mapper;

    public AppointmentPersistenceAdapter(AppointmentJpaRepository jpa, AppointmentPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Appointment save(Appointment appointment) {
        return mapper.toDomain(jpa.save(mapper.toEntity(appointment)));
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Appointment> findCurrentInProgress(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.findCurrentInProgress(clinicId, doctorId, date, PageRequest.of(0, 1))
                .stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public Optional<Appointment> findNextWaitingForUpdate(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.findNextWaitingForUpdate(clinicId, doctorId, date, PageRequest.of(0, 1))
                .stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public int countWaiting(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.countWaiting(clinicId, doctorId, date);
    }

    @Override
    public int nextQueueNumber(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.maxQueueNumber(clinicId, doctorId, date) + 1;
    }

    @Override
    public List<Appointment> findSkipped(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.findByClinicIdAndDoctorIdAndScheduledDateAndStatusOrderByQueueNumberAsc(
                        clinicId, doctorId, date, AppointmentStatus.SKIPPED)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findWaiting(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.findWaitingOrdered(clinicId, doctorId, date)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Appointment> findActiveByPatientId(Long patientId, LocalDate date) {
        return jpa.findActiveByPatientId(patientId, date).stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public Optional<Appointment> findActiveByPatientInClinic(Long patientId, Long clinicId, LocalDate date) {
        return jpa.findActiveByPatientInClinic(patientId, clinicId, date).stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveForPatientInClinicDoctor(Long patientId, Long clinicId, Long doctorId, LocalDate date) {
        return jpa.existsActive(patientId, clinicId, doctorId, date);
    }

    @Override
    public List<Appointment> findActiveByClinicAndDate(Long clinicId, LocalDate date) {
        return jpa.findAllActiveInClinicForDate(clinicId, date).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findActiveByClinicAndDoctorAndDate(Long clinicId, Long doctorId, LocalDate date) {
        return jpa.findAllActiveInClinicAndDoctorForDate(clinicId, doctorId, date).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findByClinicAndDate(Long clinicId, LocalDate date) {
        return jpa.findByClinicIdAndScheduledDate(clinicId, date).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByClinicAndDate(Long clinicId, LocalDate date) {
        return jpa.countByClinicIdAndScheduledDate(clinicId, date);
    }

    @Override
    public long countByClinicAndDateAndSource(Long clinicId, LocalDate date, BookingSource source) {
        return jpa.countByClinicIdAndScheduledDateAndBookingSource(clinicId, date, source);
    }

    @Override
    public long countByOrganizationInMonth(Long organizationId, int year, int month) {
        return jpa.countByOrgInMonth(organizationId, year, month);
    }

    @Override
    public List<Object[]> countDailyByClinicBetween(Long clinicId, LocalDate start, LocalDate end) {
        return jpa.countDailyByClinicBetween(clinicId, start, end);
    }
}