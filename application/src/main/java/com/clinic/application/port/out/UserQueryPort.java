package com.clinic.application.port.out;

import com.clinic.domain.model.PatientProfile;
import java.util.List;

public interface UserQueryPort {
    String getPatientNameById(Long patientId);
    List<PatientProfile> searchPatientsInOrg(Long organizationId, String query);
    PatientProfile getPatientProfile(Long patientId);
    boolean isPatientAccessibleByOrg(Long organizationId, Long patientId);
    List<PatientProfile> getPatientsByOrganization(Long organizationId);
    long countPatientsByOrganization(Long organizationId);
}
