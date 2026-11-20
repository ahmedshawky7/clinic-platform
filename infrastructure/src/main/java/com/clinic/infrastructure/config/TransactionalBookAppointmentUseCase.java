package com.clinic.infrastructure.config;

import com.clinic.application.command.BookAppointmentCommand;
import com.clinic.application.port.in.BookAppointmentUseCase;
import com.clinic.application.result.AppointmentResult;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionalBookAppointmentUseCase implements BookAppointmentUseCase {

    private final BookAppointmentUseCase delegate;
    private final TransactionTemplate tx;

    public TransactionalBookAppointmentUseCase(BookAppointmentUseCase delegate,
                                               TransactionTemplate tx) {
        this.delegate = delegate;
        this.tx = tx;
    }

    @Override
    public AppointmentResult book(BookAppointmentCommand command) {
        return tx.execute(status -> delegate.book(command));
    }
}