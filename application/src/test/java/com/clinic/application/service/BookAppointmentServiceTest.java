package com.clinic.application.service;

import com.clinic.application.command.BookAppointmentCommand;
import com.clinic.application.port.out.*;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.Organization;
import com.clinic.domain.model.QueueSnapshot;
import com.clinic.domain.model.enums.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookAppointmentServiceTest {

    @Mock private AppointmentRepositoryPort repository;
    @Mock private QueueEventPublisherPort eventPublisher;
    @Mock private ClinicStatusPort clinicStatusPort;
    @Mock private OrganizationRepositoryPort organizationRepo;
    @Mock private BlockedPatientPort blockedPatientPort;

    @InjectMocks private BookAppointmentService service;

    @Test
    void should_book_appointment_and_publish_snapshot_with_arrival_wait() {
        var scheduledAt = LocalDateTime.now(ZoneId.of("Africa/Cairo")).plusHours(1);
        var command = new BookAppointmentCommand(1L, 2L, 3L, scheduledAt, VisitType.EXAMINATION);
        var org = new Organization(10L, "مركز الشفاء", "shifa", OrganizationType.MEDICAL_CENTER,
                SubscriptionPlan.PRO, SubscriptionStatus.ACTIVE, true, null,
                LocalDateTime.now().plusDays(20), LocalDateTime.now());

        when(clinicStatusPort.isClinicOpen(3L)).thenReturn(true);
        when(clinicStatusPort.isWithinBookingHours(3L)).thenReturn(true);
        when(clinicStatusPort.isDoctorAssignedToClinic(3L, 2L)).thenReturn(true);
        when(blockedPatientPort.isPatientBlockedInClinic(3L, 1L)).thenReturn(false);
        when(clinicStatusPort.getOrganizationIdByClinicId(3L)).thenReturn(10L);
        when(clinicStatusPort.getClinicZoneId(3L)).thenReturn(ZoneId.of("Africa/Cairo"));
        when(clinicStatusPort.resolveVisitFee(3L, 2L, VisitType.EXAMINATION)).thenReturn(BigDecimal.valueOf(250));
        when(clinicStatusPort.calculateEstimatedWaitMinutes(3L, 2L, 6, false)).thenReturn(90L);
        when(clinicStatusPort.getDoctorArrivalTime(3L)).thenReturn("18:30");
        when(organizationRepo.findById(10L)).thenReturn(Optional.of(org));
        when(repository.countByOrganizationInMonth(eq(10L), anyInt(), anyInt())).thenReturn(5L);
        when(repository.findActiveByPatientInClinic(eq(1L), eq(3L), any())).thenReturn(Optional.empty());
        when(repository.nextQueueNumber(eq(3L), eq(2L), any())).thenReturn(7);
        when(repository.save(any())).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            return Appointment.reconstruct(100L, a.getOrganizationId(), a.getPatientId(),
                    a.getDoctorId(), a.getClinicId(), a.getQueueNumber(),
                    a.getScheduledAt(), a.getStatus(), a.getBookingSource(), a.getCreatedAt(),
                    a.getVisitType(), a.getFee(), a.isPriority());
        });
        when(repository.findCurrentInProgress(eq(3L), eq(2L), any())).thenReturn(Optional.empty());
        when(repository.countWaiting(eq(3L), eq(2L), any())).thenReturn(6);

        var result = service.book(command);

        assertThat(result.appointmentId()).isEqualTo(100L);
        assertThat(result.queueNumber()).isEqualTo(7);
        assertThat(result.fee()).isEqualByComparingTo("250");

        ArgumentCaptor<QueueSnapshot> captor = ArgumentCaptor.forClass(QueueSnapshot.class);
        verify(eventPublisher).publishQueueUpdate(captor.capture());
        assertThat(captor.getValue().waitingCount()).isEqualTo(6);
        assertThat(captor.getValue().estimatedWaitMinutes()).isEqualTo(90L);
        assertThat(captor.getValue().doctorArrivalTime()).isEqualTo("18:30");
    }

    @Test
    void should_reject_booking_outside_booking_hours() {
        var command = new BookAppointmentCommand(1L, 2L, 3L, LocalDateTime.now().plusMinutes(5));

        when(clinicStatusPort.isClinicOpen(3L)).thenReturn(true);
        when(clinicStatusPort.isWithinBookingHours(3L)).thenReturn(false);
        when(clinicStatusPort.getBookingClosedMessage(3L))
                .thenReturn("انتهى وقت استقبال الحجوزات اليوم. مواعيد الحجز المتاحة للفرع من 12:00 صباحاً إلى 08:59 مساءً.");

        assertThatThrownBy(() -> service.book(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("انتهى وقت استقبال الحجوزات");

        verify(repository, never()).save(any());
    }
}