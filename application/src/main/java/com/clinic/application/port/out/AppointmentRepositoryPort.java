package com.clinic.application.port.out;

import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.enums.BookingSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepositoryPort {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(Long id);

    Optional<Appointment> findCurrentInProgress(Long clinicId, Long doctorId, LocalDate date);
    Optional<Appointment> findNextWaitingForUpdate(Long clinicId, Long doctorId, LocalDate date);
    int countWaiting(Long clinicId, Long doctorId, LocalDate date);
    int nextQueueNumber(Long clinicId, Long doctorId, LocalDate date);
    List<Appointment> findSkipped(Long clinicId, Long doctorId, LocalDate date);
    List<Appointment> findWaiting(Long clinicId, Long doctorId, LocalDate date);

    Optional<Appointment> findActiveByPatientId(Long patientId, LocalDate date);
    Optional<Appointment> findActiveByPatientInClinic(Long patientId, Long clinicId, LocalDate date);
    boolean existsActiveForPatientInClinicDoctor(Long patientId, Long clinicId, Long doctorId, LocalDate date);

    List<Appointment> findActiveByClinicAndDate(Long clinicId, LocalDate date);
    List<Appointment> findActiveByClinicAndDoctorAndDate(Long clinicId, Long doctorId, LocalDate date);
    List<Appointment> findByClinicAndDate(Long clinicId, LocalDate date);

    long countByClinicAndDate(Long clinicId, LocalDate date);
    long countByClinicAndDateAndSource(Long clinicId, LocalDate date, BookingSource source);
    long countByOrganizationInMonth(Long organizationId, int year, int month);
    List<Object[]> countDailyByClinicBetween(Long clinicId, LocalDate start, LocalDate end);
}