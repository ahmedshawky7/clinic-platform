package com.clinic.application.port.in;

import com.clinic.domain.model.enums.VisitType;
import java.time.LocalDate;

public interface AddWalkInPatientUseCase {
    void addWalkIn(Long patientId, String fullName, LocalDate dateOfBirth,
                   String phoneNumber, Long clinicId, Long doctorId,
                   VisitType visitType, boolean priority);

    default void addWalkIn(Long patientId, String fullName, LocalDate dateOfBirth,
                           String phoneNumber, Long clinicId, Long doctorId) {
        addWalkIn(patientId, fullName, dateOfBirth, phoneNumber, clinicId, doctorId, VisitType.EXAMINATION, false);
    }
}