package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.util.PasswordGenerator;
import com.clinic.domain.model.Organization;
import com.clinic.domain.model.enums.SubscriptionPlan;
import com.clinic.domain.model.enums.SubscriptionStatus;
import com.clinic.domain.model.enums.UserRole;
import com.clinic.infrastructure.adapter.in.web.dto.StaffMemberResponse;
import com.clinic.infrastructure.adapter.out.persistence.*;
import com.clinic.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/org")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class OrganizationManagementController {

    private final OrganizationJpaRepository orgRepo;
    private final ClinicJpaRepository clinicRepo;
    private final SpecialtyJpaRepository specialtyRepo;
    private final UserJpaRepository userRepo;
    private final DoctorProfileJpaRepository doctorRepo;
    private final DoctorClinicAssignmentJpaRepository assignmentRepo;
    private final AppointmentJpaRepository appointmentRepo;
    private final PasswordEncoder passwordEncoder;

    public OrganizationManagementController(OrganizationJpaRepository orgRepo,
                                          ClinicJpaRepository clinicRepo,
                                          SpecialtyJpaRepository specialtyRepo,
                                          UserJpaRepository userRepo,
                                          DoctorProfileJpaRepository doctorRepo,
                                          DoctorClinicAssignmentJpaRepository assignmentRepo,
                                          AppointmentJpaRepository appointmentRepo,
                                          PasswordEncoder passwordEncoder) {
        this.orgRepo = orgRepo;
        this.clinicRepo = clinicRepo;
        this.specialtyRepo = specialtyRepo;
        this.userRepo = userRepo;
        this.doctorRepo = doctorRepo;
        this.assignmentRepo = assignmentRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(@AuthenticationPrincipal CustomUserDetails user) {
        Long orgId = user.getOrganizationId();
        var org = orgRepo.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));
        LocalDate today = LocalDate.now();
        var staff = userRepo.findByOrganizationIdAndRoleInOrderByIdDesc(
                        orgId, List.of(UserRole.RECEPTIONIST, UserRole.DOCTOR))
                .stream().map(StaffMemberResponse::from).toList();

        return ResponseEntity.ok(Map.of(
                "organization", org,
                "clinicsCount", clinicRepo.countByOrganizationId(orgId),
                "doctorsCount", doctorRepo.countByOrganizationId(orgId),
                "monthlyAppointments", appointmentRepo.countByOrgInMonth(orgId, today.getYear(), today.getMonthValue()),
                "clinics", clinicRepo.findByOrganizationId(orgId),
                "specialties", specialtyRepo.findByOrganizationId(orgId),
                "doctors", doctorRepo.findByOrganizationId(orgId),
                "assignments", assignmentRepo.findByOrganizationId(orgId),
                "receptionists", staff
        ));
    }

    @GetMapping("/staff")
    public ResponseEntity<List<StaffMemberResponse>> getStaff(@AuthenticationPrincipal CustomUserDetails user) {
        var staff = userRepo.findByOrganizationIdAndRoleInOrderByIdDesc(
                        user.getOrganizationId(), List.of(UserRole.RECEPTIONIST, UserRole.DOCTOR, UserRole.ORG_ADMIN))
                .stream()
                .map(StaffMemberResponse::from)
                .toList();
        return ResponseEntity.ok(staff);
    }

    @PutMapping("/staff/{staffId}/status")
    public ResponseEntity<?> toggleStaffStatus(@PathVariable Long staffId,
                                               @RequestBody Map<String, Boolean> body,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        UserEntity staff = verifyStaffOwnership(staffId, user);
        if (staff.getId().equals(user.getUserId()) || staff.isOwner()) {
            throw new IllegalStateException("لا يمكن إيقاف حساب مالك المنشأة.");
        }
        Boolean active = body.get("active");
        if (active == null) {
            throw new IllegalArgumentException("يجب تحديد حالة التفعيل (active).");
        }
        staff.setActive(active);
        userRepo.save(staff);
        return ResponseEntity.ok(StaffMemberResponse.from(staff));
    }

    @RequestMapping(value = "/staff/{staffId}/reset-password", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, String>> resetStaffPassword(@PathVariable Long staffId,
                                                                  @RequestBody(required = false) Map<String, String> body,
                                                                  @AuthenticationPrincipal CustomUserDetails user) {
        UserEntity staff = verifyStaffOwnership(staffId, user);
        String requested = (body != null) ? body.get("newPassword") : null;
        String tempPassword = (requested != null && !requested.trim().isBlank())
                ? requested.trim()
                : PasswordGenerator.generate(8);

        if (tempPassword.length() < 6) {
            throw new IllegalArgumentException("كلمة المرور يجب أن تكون 6 أحرف على الأقل.");
        }

        staff.setPassword(passwordEncoder.encode(tempPassword));
        userRepo.save(staff);

        return ResponseEntity.ok(Map.of(
                "username", staff.getUsername(),
                "fullName", staff.getFullName() != null ? staff.getFullName() : "",
                "temporaryPassword", tempPassword
        ));
    }

    @PostMapping("/clinics")
    public ResponseEntity<?> addClinic(@AuthenticationPrincipal CustomUserDetails user, @RequestBody Map<String, String> body) {
        Long orgId = user.getOrganizationId();
        OrganizationEntity orgE = orgRepo.findById(orgId).orElseThrow();
        toDomain(orgE).ensureCanAddClinic(clinicRepo.countByOrganizationId(orgId));

        ClinicEntity c = new ClinicEntity();
        c.setOrganizationId(orgId);
        c.setName(body.get("name"));
        c.setAddress(body.get("address"));
        c.setCity(body.get("city"));
        c.setPhone(body.get("phone"));
        c.setWorkingDays(body.getOrDefault("workingDays", "SATURDAY,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY"));
        c.setBookingOpenTime(body.getOrDefault("bookingOpenTime", "00:00"));
        c.setBookingCloseTime(body.getOrDefault("bookingCloseTime", "23:59"));
        return ResponseEntity.ok(clinicRepo.save(c));
    }

    @PostMapping("/specialties")
    public ResponseEntity<?> addSpecialty(@AuthenticationPrincipal CustomUserDetails user, @RequestBody Map<String, String> body) {
        SpecialtyEntity s = new SpecialtyEntity();
        s.setOrganizationId(user.getOrganizationId());
        s.setName(body.get("name"));
        s.setDescription(body.get("description"));
        return ResponseEntity.ok(specialtyRepo.save(s));
    }

    @PostMapping("/doctors")
    @Transactional
    public ResponseEntity<?> addDoctor(@AuthenticationPrincipal CustomUserDetails user, @RequestBody Map<String, Object> body) {
        Long orgId = user.getOrganizationId();
        OrganizationEntity orgE = orgRepo.findById(orgId).orElseThrow();
        toDomain(orgE).ensureCanAddDoctor(doctorRepo.countByOrganizationId(orgId));

        String username = String.valueOf(body.get("username"));
        if (userRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "اسم المستخدم مستخدم بالفعل"));
        }

        UserEntity docUser = new UserEntity();
        docUser.setUsername(username);
        docUser.setPassword(passwordEncoder.encode(String.valueOf(body.get("password"))));
        docUser.setFullName(String.valueOf(body.get("fullName")));
        docUser.setPhoneNumber(body.get("phoneNumber") != null ? String.valueOf(body.get("phoneNumber")) : null);
        docUser.setOrganizationId(orgId);
        docUser.setRole(UserRole.DOCTOR);
        docUser.setActive(true);
        docUser = userRepo.save(docUser);

        Long specialtyId = body.get("specialtyId") != null && !body.get("specialtyId").toString().isBlank()
                ? Long.valueOf(body.get("specialtyId").toString()) : null;
        String specName = specialtyId != null
                ? specialtyRepo.findById(specialtyId).map(SpecialtyEntity::getName).orElse("عام")
                : "عام";

        DoctorProfileEntity dp = new DoctorProfileEntity();
        dp.setUserId(docUser.getId());
        dp.setOrganizationId(orgId);
        dp.setFullName(docUser.getFullName());
        dp.setSpecialtyId(specialtyId);
        dp.setSpecialtyName(specName);
        if (body.get("consultationFee") != null && !body.get("consultationFee").toString().isBlank()) {
            dp.setConsultationFee(new BigDecimal(body.get("consultationFee").toString()));
        }
        if (body.get("followUpFee") != null && !body.get("followUpFee").toString().isBlank()) {
            dp.setFollowUpFee(new BigDecimal(body.get("followUpFee").toString()));
        }
        if (body.get("avgConsultationMinutes") != null) {
            dp.setAvgConsultationMinutes(Integer.parseInt(body.get("avgConsultationMinutes").toString()));
        }
        return ResponseEntity.ok(doctorRepo.save(dp));
    }

    @PostMapping("/receptionists")
    public ResponseEntity<?> addReceptionist(@AuthenticationPrincipal CustomUserDetails user, @RequestBody Map<String, String> body) {
        if (userRepo.findByUsername(body.get("username")).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "اسم المستخدم مستخدم بالفعل"));
        }
        UserEntity rec = new UserEntity();
        rec.setUsername(body.get("username"));
        rec.setPassword(passwordEncoder.encode(body.get("password")));
        rec.setFullName(body.get("fullName"));
        rec.setPhoneNumber(body.get("phoneNumber"));
        rec.setOrganizationId(user.getOrganizationId());
        rec.setRole(UserRole.RECEPTIONIST);
        rec.setActive(true);
        return ResponseEntity.ok(StaffMemberResponse.from(userRepo.save(rec)));
    }

    @PostMapping("/assignments")
    public ResponseEntity<?> assignDoctorToClinic(@AuthenticationPrincipal CustomUserDetails user, @RequestBody Map<String, Object> body) {
        Long orgId = user.getOrganizationId();
        Long clinicId = Long.valueOf(body.get("clinicId").toString());
        Long doctorId = Long.valueOf(body.get("doctorId").toString());

        ClinicEntity clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));
        if (!clinic.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("الفرع لا يتبع منشأتك");
        }
        DoctorProfileEntity dp = doctorRepo.findByUserId(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("الطبيب غير موجود"));
        if (!dp.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("الطبيب لا يتبع منشأتك");
        }

        DoctorClinicAssignmentEntity assign = assignmentRepo.findByClinicIdAndDoctorId(clinicId, doctorId)
                .orElse(new DoctorClinicAssignmentEntity());
        assign.setOrganizationId(orgId);
        assign.setClinicId(clinicId);
        assign.setDoctorId(doctorId);
        assign.setWorkingDays((String) body.getOrDefault("workingDays", clinic.getWorkingDays()));
        assign.setArrivalTime((String) body.getOrDefault("arrivalTime", "17:00"));
        assign.setDepartureTime((String) body.getOrDefault("departureTime", "22:00"));
        if (body.get("examinationFee") != null && !body.get("examinationFee").toString().isBlank()) {
            assign.setExaminationFee(new BigDecimal(body.get("examinationFee").toString()));
        }
        if (body.get("followUpFee") != null && !body.get("followUpFee").toString().isBlank()) {
            assign.setFollowUpFee(new BigDecimal(body.get("followUpFee").toString()));
        }
        assign.setActive(true);
        return ResponseEntity.ok(assignmentRepo.save(assign));
    }

    @PutMapping("/subscription")
    public ResponseEntity<?> updateSubscription(@AuthenticationPrincipal CustomUserDetails user, @RequestBody Map<String, String> body) {
        OrganizationEntity org = orgRepo.findById(user.getOrganizationId()).orElseThrow();
        org.setPlan(SubscriptionPlan.valueOf(body.get("plan")));
        org.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        org.setSubscriptionEndsAt(LocalDateTime.now().plusMonths(1));
        return ResponseEntity.ok(orgRepo.save(org));
    }

    private UserEntity verifyStaffOwnership(Long staffId, CustomUserDetails currentUser) {
        UserEntity staff = userRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("الموظف غير موجود"));
        if (staff.getOrganizationId() == null || !staff.getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new AccessDeniedException("غير مصرح لك بإدارة موظف لا يتبع منشأتك");
        }
        if (staff.getRole() == UserRole.PATIENT) {
            throw new IllegalArgumentException("هذا الحساب خاص بمريض وليس موظفاً");
        }
        return staff;
    }

    private Organization toDomain(OrganizationEntity e) {
        return new Organization(e.getId(), e.getName(), e.getSlug(), e.getType(), e.getPlan(),
                e.getSubscriptionStatus(), e.isActive(), e.getTrialEndsAt(), e.getSubscriptionEndsAt(), e.getCreatedAt());
    }
}