package com.clinic.application.port.in;

import java.time.LocalDate;

public interface RegisterUserUseCase {
    void register(String username, String password, String fullName, LocalDate dateOfBirth, String phoneNumber);
}