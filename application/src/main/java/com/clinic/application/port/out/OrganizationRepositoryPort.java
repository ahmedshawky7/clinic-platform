package com.clinic.application.port.out;

import com.clinic.domain.model.Organization;
import java.util.Optional;

public interface OrganizationRepositoryPort {
    Organization save(Organization organization);
    Optional<Organization> findById(Long id);
}