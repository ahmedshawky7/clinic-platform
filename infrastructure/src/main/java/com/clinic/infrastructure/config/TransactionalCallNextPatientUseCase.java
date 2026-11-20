package com.clinic.infrastructure.config;

import com.clinic.application.port.in.CallNextPatientUseCase;
import com.clinic.application.result.CallNextResult;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionalCallNextPatientUseCase implements CallNextPatientUseCase {
    private final CallNextPatientUseCase delegate;
    private final TransactionTemplate tx;

    public TransactionalCallNextPatientUseCase(CallNextPatientUseCase delegate, TransactionTemplate tx) {
        this.delegate = delegate;
        this.tx = tx;
    }

    @Override
    public CallNextResult callNext(Long clinicId, Long doctorId) {
        return tx.execute(status -> delegate.callNext(clinicId, doctorId));
    }
}