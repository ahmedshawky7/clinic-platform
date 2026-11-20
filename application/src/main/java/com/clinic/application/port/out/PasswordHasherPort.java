package com.clinic.application.port.out;

public interface PasswordHasherPort {
    String hash(String rawPassword);
}