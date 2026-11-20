package com.clinic.domain.model;

import com.clinic.domain.exception.InvalidAppointmentTransitionException;
import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.BookingSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class AppointmentTest {

    private Appointment newAppointment() {
        return Appointment.book(1L, 2L, 3L, 4L, 5,
                LocalDateTime.now().plusHours(1), BookingSource.ONLINE);
    }

    @Test
    void should_start_with_WAITING_status() {
        assertThat(newAppointment().getStatus()).isEqualTo(AppointmentStatus.WAITING);
    }

    @Test
    void should_transition_from_WAITING_to_IN_PROGRESS() {
        var appt = newAppointment();
        appt.startConsultation();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void should_not_start_consultation_twice() {
        var appt = newAppointment();
        appt.startConsultation();

        assertThatThrownBy(appt::startConsultation)
                .isInstanceOf(InvalidAppointmentTransitionException.class);
    }

    @Test
    void should_not_complete_a_waiting_appointment() {
        var appt = newAppointment();
        assertThatThrownBy(appt::complete)
                .isInstanceOf(InvalidAppointmentTransitionException.class);
    }

    @Test
    void should_not_cancel_a_completed_appointment() {
        var appt = newAppointment();
        appt.startConsultation();
        appt.complete();

        assertThatThrownBy(appt::cancel)
                .isInstanceOf(InvalidAppointmentTransitionException.class);
    }

    @Test
    void should_reject_zero_queue_number() {
        assertThatThrownBy(() ->
                Appointment.book(1L, 2L, 3L, 4L, 0, LocalDateTime.now().plusHours(1), BookingSource.ONLINE)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
