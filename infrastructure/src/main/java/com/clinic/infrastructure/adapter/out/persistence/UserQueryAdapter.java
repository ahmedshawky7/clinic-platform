package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.application.port.out.UserQueryPort;
import com.clinic.domain.model.PatientProfile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserQueryAdapter implements UserQueryPort {
        private final UserJpaRepository userRepository;

        public UserQueryAdapter(UserJpaRepository userRepository) {
                this.userRepository = userRepository;
        }

        @Override
        public String getPatientNameById(Long patientId) {
                return userRepository.findById(patientId).map(UserEntity::getFullName).orElse("مريض غير معروف");
        }

        @Override
        public List<PatientProfile> searchPatientsInOrg(Long organizationId, String query) {
                String normalized = query.replace('أ', 'ا').replace('إ', 'ا')
                                .replace('آ', 'ا').replace('ى', 'ي').replace('ة', 'ه');
                return userRepository.searchPatientsInOrg(organizationId, normalized).stream()
                                .map(this::toProfile).toList();
        }

        @Override
        public PatientProfile getPatientProfile(Long patientId) {
                return userRepository.findById(patientId).map(this::toProfile)
                                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        }

        @Override
        public boolean isPatientAccessibleByOrg(Long organizationId, Long patientId) {
                return userRepository.isPatientInOrg(organizationId, patientId);
        }

        @Override
        public List<PatientProfile> getPatientsByOrganization(Long organizationId) {
                return userRepository.findPatientsByOrg(organizationId).stream().map(this::toProfile).toList();
        }

        @Override
        public long countPatientsByOrganization(Long organizationId) {
                return userRepository.countPatientsByOrg(organizationId);
        }

        private PatientProfile toProfile(UserEntity u) {
                return new PatientProfile(u.getId(), u.getFullName(), u.getDateOfBirth(),
                                u.getPhoneNumber(), !u.getUsername().startsWith("walkin_"));
        }
}