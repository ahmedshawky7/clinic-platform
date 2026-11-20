package com.clinic.application.port.in;

import com.clinic.domain.model.PatientProfile;
import java.util.List;

public interface SearchPatientsUseCase {
    List<PatientProfile> search(Long organizationId, String query);
}