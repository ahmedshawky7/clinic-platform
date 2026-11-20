package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.port.in.*;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.application.util.PasswordGenerator;
import com.clinic.domain.model.PatientProfile;
import com.clinic.infrastructure.adapter.out.persistence.UserEntity;
import com.clinic.infrastructure.adapter.out.persistence.UserJpaRepository;
import com.clinic.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final GetCurrentPatientDetailsUseCase currentPatientUseCase;
    private final CompleteConsultationUseCase completeUseCase;
    private final DirectVisitUseCase directVisitUseCase;
    private final SearchPatientsUseCase searchUseCase;
    private final GetPatientFullFileUseCase fullFileUseCase;
    private final GeneratePatientAccountUseCase generateAccountUseCase;
    private final UserQueryPort userQueryPort;
    private final UserJpaRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DoctorController(GetCurrentPatientDetailsUseCase currentPatientUseCase,
                            CompleteConsultationUseCase completeUseCase,
                            DirectVisitUseCase directVisitUseCase,
                            SearchPatientsUseCase searchUseCase,
                            GetPatientFullFileUseCase fullFileUseCase,
                            GeneratePatientAccountUseCase generateAccountUseCase,
                            UserQueryPort userQueryPort,
                            UserJpaRepository userRepo,
                            PasswordEncoder passwordEncoder) {
        this.currentPatientUseCase = currentPatientUseCase;
        this.completeUseCase = completeUseCase;
        this.directVisitUseCase = directVisitUseCase;
        this.searchUseCase = searchUseCase;
        this.fullFileUseCase = fullFileUseCase;
        this.generateAccountUseCase = generateAccountUseCase;
        this.userQueryPort = userQueryPort;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/clinics/{clinicId}/current-patient-details")
    public GetCurrentPatientDetailsUseCase.PatientDetails currentPatientDetails(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long clinicId) {
        return currentPatientUseCase.getCurrentPatientDetails(clinicId, currentUser.getUserId());
    }

    @PostMapping("/clinics/{clinicId}/complete-current")
    public void completeCurrent(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @PathVariable Long clinicId,
                                @RequestBody Map<String, String> body) {
        var current = currentPatientUseCase.getCurrentPatientDetails(clinicId, currentUser.getUserId());
        completeUseCase.complete(current.appointmentId(), body.get("diagnosis"), body.get("medications"));
    }

    @PostMapping("/clinics/{clinicId}/direct-visit")
    public void directVisit(@AuthenticationPrincipal CustomUserDetails currentUser,
                            @PathVariable Long clinicId,
                            @RequestBody Map<String, String> body) {
        Long patientId = Long.valueOf(body.get("patientId"));
        directVisitUseCase.addDirectVisit(patientId, clinicId, currentUser.getUserId(),
                body.get("diagnosis"), body.get("medications"));
    }

    @GetMapping("/patients/search")
    public List<PatientProfile> search(@AuthenticationPrincipal CustomUserDetails currentUser,
                                       @RequestParam String query) {
        return searchUseCase.search(currentUser.getOrganizationId(), query);
    }

    @GetMapping("/patients/{patientId}/file")
    public GetPatientFullFileUseCase.PatientFileDetails fullFile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                 @PathVariable Long patientId) {
        return fullFileUseCase.getPatientFile(currentUser.getOrganizationId(), patientId);
    }

    @GetMapping("/patients/all")
    public List<PatientProfile> allPatients(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return userQueryPort.getPatientsByOrganization(currentUser.getOrganizationId());
    }

    @PostMapping("/patients/{patientId}/generate-account")
    public GeneratePatientAccountUseCase.AccountCredentials generateAccount(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long patientId) {
        return generateAccountUseCase.generate(currentUser.getOrganizationId(), patientId);
    }

    @PostMapping("/patients/{patientId}/reset-password")
    public ResponseEntity<Map<String, String>> resetPatientPassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long patientId) {
        if (!userQueryPort.isPatientAccessibleByOrg(currentUser.getOrganizationId(), patientId)) {
            throw new AccessDeniedException("هذا المريض غير مسجل لدى منشأتك");
        }
        UserEntity patient = userRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("المريض غير موجود"));

        if (patient.getUsername().startsWith("walkin_")) {
            throw new IllegalStateException("هذا المريض ليس لديه حساب مفعّل بعد. استخدم زر إنشاء حساب أولاً.");
        }

        String tempPassword = PasswordGenerator.generate(8);
        patient.setPassword(passwordEncoder.encode(tempPassword));
        userRepo.save(patient);

        return ResponseEntity.ok(Map.of(
                "username", patient.getUsername(),
                "fullName", patient.getFullName() != null ? patient.getFullName() : "",
                "temporaryPassword", tempPassword
        ));
    }
}