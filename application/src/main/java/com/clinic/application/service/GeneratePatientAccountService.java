package com.clinic.application.service;

import com.clinic.application.port.in.GeneratePatientAccountUseCase;
import com.clinic.application.port.out.PasswordHasherPort;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.application.port.out.UserRegistrationPort;
import com.clinic.application.util.PasswordGenerator;

public class GeneratePatientAccountService implements GeneratePatientAccountUseCase {
    private final UserRegistrationPort userRepo;
    private final UserQueryPort userQueryPort;
    private final PasswordHasherPort passwordHasher;

    public GeneratePatientAccountService(UserRegistrationPort userRepo,
                                         UserQueryPort userQueryPort,
                                         PasswordHasherPort passwordHasher) {
        this.userRepo = userRepo;
        this.userQueryPort = userQueryPort;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public AccountCredentials generate(Long organizationId, Long patientId) {
        if (!userQueryPort.isPatientAccessibleByOrg(organizationId, patientId)) {
            throw new IllegalArgumentException("هذا المريض غير مسجل لدى منشأتك");
        }
        var profile = userQueryPort.getPatientProfile(patientId);
        if (profile.hasAccount()) {
            throw new IllegalStateException("هذا المريض يمتلك حساباً مفعّلاً بالفعل. يمكنك استخدام إعادة تعيين كلمة المرور.");
        }

        String newUsername = "patient_" + patientId;
        String rawPassword = PasswordGenerator.generate(8);
        String hashedPassword = passwordHasher.hash(rawPassword);
        userRepo.updatePatientToRealAccount(patientId, newUsername, hashedPassword);
        return new AccountCredentials(newUsername, rawPassword);
    }
}