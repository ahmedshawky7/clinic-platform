package com.clinic.application.service;

import com.clinic.application.port.in.RegisterUserUseCase;
import com.clinic.application.port.out.PasswordHasherPort;
import com.clinic.application.port.out.UserRegistrationPort;
import java.time.LocalDate;

public class RegisterUserService implements RegisterUserUseCase {
    private final UserRegistrationPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public RegisterUserService(UserRegistrationPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void register(String username, String password, String fullName, LocalDate dateOfBirth, String phoneNumber) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("اسم المستخدم موجود بالفعل");
        }
        String hashedPassword = passwordHasher.hash(password);
        userRepository.savePatient(username, hashedPassword, fullName, dateOfBirth, phoneNumber, null);
    }
}