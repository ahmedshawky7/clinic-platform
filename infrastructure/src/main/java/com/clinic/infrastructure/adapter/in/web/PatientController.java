package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.port.in.GetPatientPrescriptionsUseCase;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.application.port.out.UserRegistrationPort;
import com.clinic.domain.model.Prescription;
import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.infrastructure.adapter.out.persistence.*;
import com.clinic.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final GetPatientPrescriptionsUseCase getPrescriptionsUseCase;
    private final UserQueryPort userQueryPort;
    private final UserRegistrationPort userRegistrationPort;
    private final AppointmentJpaRepository appointmentJpa;
    private final ClinicJpaRepository clinicRepo;
    private final DoctorProfileJpaRepository doctorRepo;
    private final ClinicStatusPort clinicStatusPort;

    public PatientController(GetPatientPrescriptionsUseCase getPrescriptionsUseCase,
                             UserQueryPort userQueryPort,
                             UserRegistrationPort userRegistrationPort,
                             AppointmentJpaRepository appointmentJpa,
                             ClinicJpaRepository clinicRepo,
                             DoctorProfileJpaRepository doctorRepo,
                             ClinicStatusPort clinicStatusPort) {
        this.getPrescriptionsUseCase = getPrescriptionsUseCase;
        this.userQueryPort = userQueryPort;
        this.userRegistrationPort = userRegistrationPort;
        this.appointmentJpa = appointmentJpa;
        this.clinicRepo = clinicRepo;
        this.doctorRepo = doctorRepo;
        this.clinicStatusPort = clinicStatusPort;
    }

    @GetMapping("/my-active-appointment")
    public ResponseEntity<?> getMyLatestAppointmentToday(@AuthenticationPrincipal CustomUserDetails currentUser) {
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Cairo"));
        var latestOpt = appointmentJpa.findLatestByPatientIdAndDate(currentUser.getUserId(), today)
                .stream().findFirst();
        if (latestOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        AppointmentEntity a = latestOpt.get();
        ClinicEntity clinic = clinicRepo.findById(a.getClinicId()).orElse(null);
        String clinicName = clinic != null ? clinic.getName() : "";
        String closureReason = clinic != null && clinic.getClosureReason() != null ? clinic.getClosureReason() : "";
        String doctorName = doctorRepo.findByUserId(a.getDoctorId()).map(DoctorProfileEntity::getFullName).orElse("");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appointmentId", a.getId());
        body.put("clinicId", a.getClinicId());
        body.put("clinicName", clinicName);
        body.put("doctorId", a.getDoctorId());
        body.put("doctorName", doctorName);
        body.put("queueNumber", a.getQueueNumber() != null ? a.getQueueNumber() : 0);
        body.put("status", a.getStatus().name());
        body.put("closureReason", closureReason);
        body.put("visitType", a.getVisitType() != null ? a.getVisitType().name() : "EXAMINATION");
        body.put("fee", a.getFee() != null ? a.getFee() : BigDecimal.ZERO);
        body.put("priority", a.isPriority());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/appointments/history")
    public ResponseEntity<List<Map<String, Object>>> getAppointmentsHistory(@AuthenticationPrincipal CustomUserDetails currentUser) {
        var list = appointmentJpa.findByPatientIdOrderByScheduledDateDescCreatedAtDesc(currentUser.getUserId());
        var result = list.stream().map(a -> {
            ZoneId zone = clinicStatusPort.getClinicZoneId(a.getClinicId());
            LocalDate todayInClinic = LocalDate.now(zone);

            AppointmentStatus effectiveStatus = a.getStatus();
            if (a.getScheduledDate().isBefore(todayInClinic)
                    && (effectiveStatus == AppointmentStatus.WAITING || effectiveStatus == AppointmentStatus.SKIPPED)) {
                effectiveStatus = AppointmentStatus.NO_SHOW;
            }

            String clinicName = clinicRepo.findById(a.getClinicId()).map(ClinicEntity::getName).orElse("فرع غير معروف");
            String doctorName = doctorRepo.findByUserId(a.getDoctorId()).map(DoctorProfileEntity::getFullName).orElse("طبيب");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("appointmentId", a.getId());
            row.put("clinicId", a.getClinicId());
            row.put("clinicName", clinicName);
            row.put("doctorId", a.getDoctorId());
            row.put("doctorName", doctorName);
            row.put("queueNumber", a.getQueueNumber() != null ? a.getQueueNumber() : 0);
            row.put("scheduledDate", a.getScheduledDate().toString());
            row.put("scheduledAt", a.getScheduledAt().toString());
            row.put("status", effectiveStatus.name());
            row.put("visitType", a.getVisitType() != null ? a.getVisitType().name() : "EXAMINATION");
            row.put("fee", a.getFee() != null ? a.getFee() : BigDecimal.ZERO);
            row.put("priority", a.isPriority());
            return row;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<List<Prescription>> getMyPrescriptions(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(getPrescriptionsUseCase.getPrescriptions(user.getUserId()));
    }

    @GetMapping("/{patientId}/prescriptions")
    public ResponseEntity<List<Prescription>> getPrescriptions(@PathVariable Long patientId,
                                                               @AuthenticationPrincipal CustomUserDetails user) {
        verifySelf(patientId, user);
        return ResponseEntity.ok(getPrescriptionsUseCase.getPrescriptions(patientId));
    }

    @GetMapping("/{patientId}/profile")
    public ResponseEntity<?> getMyProfile(@PathVariable Long patientId,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        verifySelf(patientId, user);
        return ResponseEntity.ok(userQueryPort.getPatientProfile(patientId));
    }

    @PutMapping("/{patientId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long patientId,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        verifySelf(patientId, user);
        userRegistrationPort.updateProfile(patientId, body.get("fullName"), body.get("phoneNumber"));
        return ResponseEntity.ok(Map.of("message", "تم تحديث الملف الشخصي بنجاح"));
    }

    private void verifySelf(Long patientId, CustomUserDetails user) {
        if (!user.getUserId().equals(patientId)) {
            throw new AccessDeniedException("غير مصرح لك بالوصول لبيانات مريض آخر");
        }
    }
}