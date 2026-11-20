package com.clinic.application.command;

public record RegisterCommand(String username, String password, String fullName) {}