package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.util.PasswordGenerator;
import com.clinic.domain.model.enums.SubscriptionStatus;
import com.clinic.infrastructure.adapter.out.persistence.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    private final OrganizationJpaRepository orgRepo;
    private final UserJpaRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminController(OrganizationJpaRepository orgRepo,
                                UserJpaRepository userRepo,
                                PasswordEncoder passwordEncoder) {
        this.orgRepo = orgRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/organizations")
    public List<OrganizationEntity> listOrganizations() {
        return orgRepo.findAll();
    }

    @PutMapping("/organizations/{id}/subscription")
    public OrganizationEntity updateSubscription(@PathVariable Long id, @RequestBody Map<String, String> body) {
        OrganizationEntity org = orgRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));
        SubscriptionStatus status = SubscriptionStatus.valueOf(body.get("status"));
        org.setSubscriptionStatus(status);
        org.setActive(status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL);
        return orgRepo.save(org);
    }

    @PutMapping("/organizations/{id}/status")
    public OrganizationEntity updateActiveStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        OrganizationEntity org = orgRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));
        boolean active = Boolean.TRUE.equals(body.get("active"));
        org.setActive(active);
        return orgRepo.save(org);
    }

    @PostMapping("/organizations/{id}/reset-admin-password")
    public ResponseEntity<Map<String, String>> resetOrgAdminPassword(@PathVariable Long id) {
        UserEntity admin = userRepo.findFirstByOrganizationIdAndOwnerTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("لم يتم العثور على مدير لهذه المنشأة"));

        String tempPassword = PasswordGenerator.generate(8);
        admin.setPassword(passwordEncoder.encode(tempPassword));
        userRepo.save(admin);

        return ResponseEntity.ok(Map.of(
                "username", admin.getUsername(),
                "fullName", admin.getFullName() != null ? admin.getFullName() : "",
                "temporaryPassword", tempPassword
        ));
    }
}