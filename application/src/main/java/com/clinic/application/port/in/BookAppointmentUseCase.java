package com.clinic.application.port.in;

import com.clinic.application.command.BookAppointmentCommand;
import com.clinic.application.result.AppointmentResult;

public interface BookAppointmentUseCase {
    AppointmentResult book(BookAppointmentCommand command);
}