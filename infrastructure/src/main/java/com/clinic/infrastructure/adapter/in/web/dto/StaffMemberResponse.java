package com.clinic.infrastructure.adapter.in.web.dto;

import com.clinic.infrastructure.adapter.out.persistence.UserEntity;

public record StaffMemberResponse(
        Long id,
        String username,
        String fullName,
        String phoneNumber,
        String role,
        boolean active,
        boolean owner
) {
    public static StaffMemberResponse from(UserEntity u) {
        return new StaffMemberResponse(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getPhoneNumber(),
                u.getRole().name(),
                u.isActive(),
                u.isOwner()
        );
    }
}
