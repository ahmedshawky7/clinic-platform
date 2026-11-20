package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.domain.model.enums.UserRole;
import com.clinic.infrastructure.adapter.in.web.dto.ClinicDetailsResponse;
import com.clinic.infrastructure.adapter.out.persistence.*;
import com.clinic.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clinics")
public class ClinicController {

    private final ClinicJpaRepository clinicRepo;
    private final DoctorClinicAssignmentJpaRepository assignmentRepo;
    private final DoctorProfileJpaRepository doctorRepo;
    private final ClinicStatusPort clinicStatusPort;
    private final BlockedPatientJpaRepository blockedRepo;
    private final UserJpaRepository userRepo;

    public ClinicController(ClinicJpaRepository clinicRepo,
                            DoctorClinicAssignmentJpaRepository assignmentRepo,
                            DoctorProfileJpaRepository doctorRepo,
                            ClinicStatusPort clinicStatusPort,
                            BlockedPatientJpaRepository blockedRepo,
                            UserJpaRepository userRepo) {
        this.clinicRepo = clinicRepo;
        this.assignmentRepo = assignmentRepo;
        this.doctorRepo = doctorRepo;
        this.clinicStatusPort = clinicStatusPort;
        this.blockedRepo = blockedRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> getMyClinics(@AuthenticationPrincipal CustomUserDetails user) {
        List<ClinicEntity> clinics = clinicRepo.findByOrganizationId(user.getOrganizationId());
        var result = clinics.stream().map(c -> {
            var assignments = assignmentRepo.findByClinicIdAndActiveTrue(c.getId());
            if (user.getRole() == UserRole.DOCTOR && !user.isOwner()) {
                assignments = assignments.stream()
                        .filter(a -> a.getDoctorId().equals(user.getUserId()))
                        .toList();
            }
            var docIds = assignments.stream().map(DoctorClinicAssignmentEntity::getDoctorId).toList();
            var doctors = docIds.isEmpty() ? List.of() : doctorRepo.findByUserIdIn(docIds);
            return Map.of(
                    "clinic", c,
                    "doctors", doctors,
                    "assignments", assignments
            );
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<?> getClinicInfo(@PathVariable Long id) {
        return clinicRepo.findById(id).map(c -> {
            boolean open = clinicStatusPort.isClinicOpen(id);
            boolean withinHours = clinicStatusPort.isWithinBookingHours(id);
            String msg = "";
            if (!open) {
                msg = clinicStatusPort.getClosureReason(id);
            } else if (!withinHours) {
                msg = clinicStatusPort.getBookingClosedMessage(id);
            }
            return ResponseEntity.ok(ClinicDetailsResponse.from(c, open, withinHours, msg));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> toggleClinicStatus(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal CustomUserDetails user) {
        ClinicEntity c = verifyClinicOwnership(id, user);
        boolean open = Boolean.TRUE.equals(body.get("isOpen"));
        c.setOpenToday(open);
        c.setClosureReason(open ? null : String.valueOf(body.getOrDefault("reason", "")));
        clinicRepo.save(c);
        return ResponseEntity.ok(Map.of("message", "تم التحديث"));
    }

    @PutMapping("/{id}/info")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> updateClinicInfo(@PathVariable Long id,
                                              @RequestBody Map<String, String> body,
                                              @AuthenticationPrincipal CustomUserDetails user) {
        ClinicEntity c = verifyClinicOwnership(id, user);
        if (body.containsKey("workingDays")) c.setWorkingDays(body.get("workingDays"));
        if (body.containsKey("address")) c.setAddress(body.get("address"));
        if (body.containsKey("city")) c.setCity(body.get("city"));
        if (body.containsKey("name") && body.get("name") != null && !body.get("name").isBlank()) {
            c.setName(body.get("name"));
        }
        if (body.containsKey("timezone")) c.setTimezone(body.get("timezone"));
        clinicRepo.save(c);
        return ResponseEntity.ok(Map.of("message", "تم التحديث"));
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        ClinicEntity c = verifyClinicOwnership(id, user);
        if (body.containsKey("bookingOpenTime")) c.setBookingOpenTime(body.get("bookingOpenTime"));
        if (body.containsKey("bookingCloseTime")) c.setBookingCloseTime(body.get("bookingCloseTime"));
        if (body.containsKey("doctorArrivalTime")) c.setDoctorArrivalTime(body.get("doctorArrivalTime"));
        clinicRepo.save(c);
        return ResponseEntity.ok(Map.of("message", "تم تحديث المواعيد"));
    }

    @GetMapping("/{clinicId}/blocked-patients")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public List<BlockedPatientEntity> getBlockedPatients(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                           @PathVariable Long clinicId) {
        verifyClinicOwnership(clinicId, currentUser);
        return blockedRepo.findByClinicIdAndActiveTrueOrderByCreatedAtDesc(clinicId);
    }

    @PostMapping("/{clinicId}/blocked-patients")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public BlockedPatientEntity blockPatient(@AuthenticationPrincipal CustomUserDetails currentUser,
                                             @PathVariable Long clinicId,
                                             @RequestBody Map<String, Object> body) {
        ClinicEntity clinic = verifyClinicOwnership(clinicId, currentUser);
        Long patientId = body.get("patientId") != null && !String.valueOf(body.get("patientId")).isBlank()
                ? Long.valueOf(String.valueOf(body.get("patientId"))) : null;
        String phone = body.get("phoneNumber") != null ? String.valueOf(body.get("phoneNumber")).trim() : "";
        String patientName = body.get("patientName") != null ? String.valueOf(body.get("patientName")) : "";

        if (patientId != null) {
            var userOpt = userRepo.findById(patientId);
            if (userOpt.isPresent()) {
                if (phone.isBlank() && userOpt.get().getPhoneNumber() != null) {
                    phone = userOpt.get().getPhoneNumber().trim();
                }
                if (patientName.isBlank()) {
                    patientName = userOpt.get().getFullName();
                }
            }
        }

        if (phone.isBlank() && patientId == null) {
            throw new IllegalArgumentException("يرجى تحديد المريض أو رقم الهاتف المراد حظره");
        }
        if (phone.isBlank()) {
            phone = "PATIENT_" + patientId;
        }

        if ((patientId != null && blockedRepo.existsByClinicIdAndPatientIdAndActiveTrue(clinicId, patientId))
                || blockedRepo.existsByClinicIdAndPhoneNumberAndActiveTrue(clinicId, phone)) {
            throw new IllegalStateException("هذا المريض محظور بالفعل في هذا الفرع");
        }

        BlockedPatientEntity entity = new BlockedPatientEntity();
        entity.setOrganizationId(clinic.getOrganizationId());
        entity.setClinicId(clinicId);
        entity.setPatientId(patientId);
        entity.setPhoneNumber(phone);
        entity.setPatientName(patientName);
        entity.setReason(body.get("reason") != null ? String.valueOf(body.get("reason")) : "مخالفة شروط الحجز");
        entity.setBlockedByUserId(currentUser.getUserId());
        entity.setActive(true);
        return blockedRepo.save(entity);
    }

    @DeleteMapping("/{clinicId}/blocked-patients/{blockId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> unblockPatientById(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                @PathVariable Long clinicId,
                                                @PathVariable Long blockId) {
        verifyClinicOwnership(clinicId, currentUser);
        BlockedPatientEntity entity = blockedRepo.findByIdAndOrganizationId(blockId, currentUser.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("السجل غير موجود"));
        if (!entity.getClinicId().equals(clinicId)) {
            throw new AccessDeniedException("غير مصرح");
        }
        entity.setActive(false);
        blockedRepo.save(entity);
        return ResponseEntity.ok(Map.of("message", "تم فك الحظر بنجاح"));
    }

    @DeleteMapping("/{clinicId}/blocked-patients")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> unblockPatientByPhone(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                   @PathVariable Long clinicId,
                                                   @RequestParam String phoneNumber) {
        verifyClinicOwnership(clinicId, currentUser);
        String target = phoneNumber.trim();
        var matches = blockedRepo.findByClinicIdAndActiveTrueOrderByCreatedAtDesc(clinicId).stream()
                .filter(b -> target.equals(b.getPhoneNumber()))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("رقم الهاتف غير محظور في هذا الفرع");
        }
        for (BlockedPatientEntity entity : matches) {
            entity.setActive(false);
            blockedRepo.save(entity);
        }
        return ResponseEntity.ok(Map.of("message", "تم فك الحظر بنجاح"));
    }

    private ClinicEntity verifyClinicOwnership(Long clinicId, CustomUserDetails user) {
        ClinicEntity c = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));
        if (!c.getOrganizationId().equals(user.getOrganizationId())) {
            throw new AccessDeniedException("غير مصرح لك بتعديل بيانات فرع لمنشأة أخرى");
        }
        return c;
    }
}