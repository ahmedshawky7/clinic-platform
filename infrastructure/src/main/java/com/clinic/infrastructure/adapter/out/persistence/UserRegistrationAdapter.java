package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.application.port.out.UserRegistrationPort;
import com.clinic.domain.model.enums.UserRole;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class UserRegistrationAdapter implements UserRegistrationPort {
    private final UserJpaRepository userRepository;

    public UserRegistrationAdapter(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    public Long savePatient(String username, String encodedPassword, String fullName,
            LocalDate dateOfBirth, String phoneNumber, Long registeredByOrgId) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setFullName(fullName);
        user.setDateOfBirth(dateOfBirth);
        user.setPhoneNumber(phoneNumber);
        user.setOrganizationId(registeredByOrgId);
        user.setRole(UserRole.PATIENT);
        return userRepository.save(user).getId();
    }

    @Override
    public void updatePatientToRealAccount(Long patientId, String newUsername, String hashedPassword) {
        UserEntity user = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        user.setUsername(newUsername);
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    @Override
    public void updateProfile(Long patientId, String fullName, String phoneNumber) {
        UserEntity user = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);
    }
}