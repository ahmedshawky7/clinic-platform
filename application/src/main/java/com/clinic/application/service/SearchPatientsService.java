package com.clinic.application.service;

import com.clinic.application.port.in.SearchPatientsUseCase;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.domain.model.PatientProfile;
import java.util.List;

public class SearchPatientsService implements SearchPatientsUseCase {
    private final UserQueryPort userQueryPort;

    public SearchPatientsService(UserQueryPort userQueryPort) {
        this.userQueryPort = userQueryPort;
    }

    @Override
    public List<PatientProfile> search(Long organizationId, String query) {
        return userQueryPort.searchPatientsInOrg(organizationId, query);
    }
}