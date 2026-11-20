package com.clinic.infrastructure.config;

import com.clinic.application.port.in.CompleteConsultationUseCase;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionalCompleteConsultationUseCase implements CompleteConsultationUseCase {

    private final CompleteConsultationUseCase delegate;
    private final TransactionTemplate tx;

    public TransactionalCompleteConsultationUseCase(CompleteConsultationUseCase delegate, TransactionTemplate tx) {
        this.delegate = delegate;
        this.tx = tx;
    }

    @Override
    public void complete(Long appointmentId, String diagnosis, String medications) {
        tx.executeWithoutResult(status -> delegate.complete(appointmentId, diagnosis, medications));
    }
}