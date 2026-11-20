package com.clinic.application.port.in;

import com.clinic.domain.model.enums.VisitType;

public interface DirectVisitUseCase {
    void addDirectVisit(Long patientId, Long clinicId, Long doctorId,
                        String diagnosis, String medications, VisitType visitType);

    default void addDirectVisit(Long patientId, Long clinicId, Long doctorId,
                                String diagnosis, String medications) {
        addDirectVisit(patientId, clinicId, doctorId, diagnosis, medications, VisitType.EXAMINATION);
    }
}