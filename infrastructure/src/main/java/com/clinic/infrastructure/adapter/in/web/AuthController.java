package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.port.in.RegisterUserUseCase;
import com.clinic.domain.model.enums.*;
import com.clinic.infrastructure.adapter.out.persistence.*;
import com.clinic.infrastructure.security.CustomUserDetails;
import com.clinic.infrastructure.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RegisterUserUseCase registerUserUseCase;
    private final UserJpaRepository userRepo;
    private final OrganizationJpaRepository orgRepo;
    private final ClinicJpaRepository clinicRepo;
    private final SpecialtyJpaRepository specialtyRepo;
    private final DoctorProfileJpaRepository doctorRepo;
    private final DoctorClinicAssignmentJpaRepository assignmentRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            RegisterUserUseCase registerUserUseCase,
            UserJpaRepository userRepo,
            OrganizationJpaRepository orgRepo,
            ClinicJpaRepository clinicRepo,
            SpecialtyJpaRepository specialtyRepo,
            DoctorProfileJpaRepository doctorRepo,
            DoctorClinicAssignmentJpaRepository assignmentRepo,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.registerUserUseCase = registerUserUseCase;
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.clinicRepo = clinicRepo;
        this.specialtyRepo = specialtyRepo;
        this.doctorRepo = doctorRepo;
        this.assignmentRepo = assignmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public record LoginRequest(String username, String password) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    public record RegisterPatientRequest(String username, String password, String fullName, LocalDate dateOfBirth,
            String phoneNumber) {
    }

    public record RegisterWorkspaceRequest(
            String username, String password, String fullName, String phoneNumber,
            String organizationName, OrganizationType organizationType,
            String firstBranchName, String city, String address,
            String specialtyName, SubscriptionPlan plan) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (LockedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "تم إيقاف حساب المنشأة من قِبل إدارة المنصة. يرجى التواصل مع الدعم."));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "تم إيقاف هذا الحساب من قِبل الإدارة."));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "اسم المستخدم أو كلمة المرور غير صحيحة"));
        }

        var userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);
        if (userDetails instanceof CustomUserDetails cud) {
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "userId", cud.getUserId(),
                    "role", cud.getRole().name(),
                    "username", cud.getUsername()));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@RequestBody RegisterPatientRequest req) {
        registerUserUseCase.register(req.username(), req.password(), req.fullName(), req.dateOfBirth(),
                req.phoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "تم إنشاء الحساب بنجاح"));
    }

    @PostMapping("/register-workspace")
    @Transactional
    public ResponseEntity<?> registerWorkspace(@RequestBody RegisterWorkspaceRequest req) {
        if (userRepo.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("اسم المستخدم موجود بالفعل");
        }

        OrganizationEntity org = new OrganizationEntity();
        org.setName(req.organizationName());
        org.setSlug("org-" + UUID.randomUUID().toString().substring(0, 8));
        org.setType(req.organizationType() != null ? req.organizationType() : OrganizationType.SOLO_DOCTOR);
        org.setPlan(req.plan() != null ? req.plan() : SubscriptionPlan.TRIAL);
        org.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        org.setActive(true);
        org.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        org = orgRepo.save(org);

        UserEntity owner = new UserEntity();
        owner.setUsername(req.username());
        owner.setPassword(passwordEncoder.encode(req.password()));
        owner.setFullName(req.fullName());
        owner.setPhoneNumber(req.phoneNumber());
        owner.setOrganizationId(org.getId());
        owner.setOwner(true);
        owner.setActive(true);
        owner.setRole(org.getType() == OrganizationType.SOLO_DOCTOR ? UserRole.DOCTOR : UserRole.ORG_ADMIN);
        owner = userRepo.save(owner);

        ClinicEntity clinic = new ClinicEntity();
        clinic.setOrganizationId(org.getId());
        clinic.setName(req.firstBranchName() != null && !req.firstBranchName().isBlank() ? req.firstBranchName()
                : "الفرع الرئيسي");
        clinic.setCity(req.city());
        clinic.setAddress(req.address());
        clinic = clinicRepo.save(clinic);

        String specName = (req.specialtyName() != null && !req.specialtyName().isBlank()) ? req.specialtyName()
                : "طب عام";
        SpecialtyEntity spec = new SpecialtyEntity();
        spec.setOrganizationId(org.getId());
        spec.setName(specName);
        spec = specialtyRepo.save(spec);

        if (org.getType() == OrganizationType.SOLO_DOCTOR) {
            DoctorProfileEntity dp = new DoctorProfileEntity();
            dp.setUserId(owner.getId());
            dp.setOrganizationId(org.getId());
            dp.setSpecialtyId(spec.getId());
            dp.setSpecialtyName(spec.getName());
            dp.setFullName(owner.getFullName());
            doctorRepo.save(dp);

            DoctorClinicAssignmentEntity assign = new DoctorClinicAssignmentEntity();
            assign.setOrganizationId(org.getId());
            assign.setClinicId(clinic.getId());
            assign.setDoctorId(owner.getId());
            assign.setWorkingDays(clinic.getWorkingDays());
            assignmentRepo.save(assign);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "تم إنشاء المنشأة بنجاح"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody ChangePasswordRequest req) {
        if (req.newPassword() == null || req.newPassword().trim().length() < 6) {
            throw new IllegalArgumentException("كلمة المرور الجديدة يجب أن تكون 6 أحرف على الأقل");
        }
        UserEntity user = userRepo.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("المستخدم غير موجود"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("كلمة المرور الحالية غير صحيحة");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword().trim()));
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "تم تغيير كلمة المرور بنجاح"));
    }
}