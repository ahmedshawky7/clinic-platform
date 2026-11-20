package com.clinic.application.port.in;

import com.clinic.application.result.CallNextResult;

public interface CallNextPatientUseCase {
    CallNextResult callNext(Long clinicId, Long doctorId);
}