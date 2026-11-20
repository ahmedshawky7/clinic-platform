package com.clinic.infrastructure.adapter.in.web;

import com.clinic.domain.model.enums.SubscriptionStatus;
import com.clinic.infrastructure.adapter.out.persistence.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/public")
public class PublicDiscoveryController {

    private final OrganizationJpaRepository orgRepo;
    private final ClinicJpaRepository clinicRepo;
    private final SpecialtyJpaRepository specialtyRepo;
    private final DoctorProfileJpaRepository doctorRepo;
    private final DoctorClinicAssignmentJpaRepository assignmentRepo;

    public PublicDiscoveryController(OrganizationJpaRepository orgRepo,
                                     ClinicJpaRepository clinicRepo,
                                     SpecialtyJpaRepository specialtyRepo,
                                     DoctorProfileJpaRepository doctorRepo,
                                     DoctorClinicAssignmentJpaRepository assignmentRepo) {
        this.orgRepo = orgRepo;
        this.clinicRepo = clinicRepo;
        this.specialtyRepo = specialtyRepo;
        this.doctorRepo = doctorRepo;
        this.assignmentRepo = assignmentRepo;
    }

    @GetMapping("/organizations")
    public ResponseEntity<?> listActiveOrganizations() {
        var active = orgRepo.findAll().stream()
                .filter(OrganizationEntity::isActive)
                .filter(o -> o.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                        || o.getSubscriptionStatus() == SubscriptionStatus.TRIAL)
                .map(o -> Map.of(
                        "id", o.getId(),
                        "name", o.getName(),
                        "type", o.getType(),
                        "clinics", clinicRepo.findByOrganizationId(o.getId()),
                        "specialties", specialtyRepo.findByOrganizationId(o.getId())
                )).toList();
        return ResponseEntity.ok(active);
    }

    @GetMapping("/clinics")
    public ResponseEntity<List<Map<String, Object>>> listAvailableClinics() {
        List<OrganizationEntity> activeOrgs = orgRepo.findAll().stream()
                .filter(OrganizationEntity::isActive)
                .filter(o -> o.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                        || o.getSubscriptionStatus() == SubscriptionStatus.TRIAL)
                .toList();

        List<Map<String, Object>> out = new ArrayList<>();
        for (OrganizationEntity org : activeOrgs) {
            for (ClinicEntity clinic : clinicRepo.findByOrganizationId(org.getId())) {
                List<Long> docIds = assignmentRepo.findByClinicIdAndActiveTrue(clinic.getId())
                        .stream().map(DoctorClinicAssignmentEntity::getDoctorId).toList();
                List<DoctorProfileEntity> doctors = docIds.isEmpty() ? List.of() : doctorRepo.findByUserIdIn(docIds);
                out.add(Map.of(
                        "organizationId", org.getId(),
                        "organizationName", org.getName(),
                        "clinic", clinic,
                        "doctors", doctors,
                        "openToday", clinic.isOpenToday(),
                        "closureReason", clinic.getClosureReason() != null ? clinic.getClosureReason() : ""
                ));
            }
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/clinics/{clinicId}/tv-info")
    public ResponseEntity<?> getClinicTvInfo(@PathVariable Long clinicId) {
        var clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));
        var org = orgRepo.findById(clinic.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));

        var assignments = assignmentRepo.findByClinicIdAndActiveTrue(clinicId);
        var doctorIds = assignments.stream().map(DoctorClinicAssignmentEntity::getDoctorId).toList();
        List<Map<String, Object>> doctors = doctorIds.isEmpty()
                ? List.of()
                : doctorRepo.findByUserIdIn(doctorIds).stream()
                        .map(d -> Map.<String, Object>of(
                                "doctorId", d.getUserId(),
                                "fullName", d.getFullName() != null ? d.getFullName() : "",
                                "specialtyName", d.getSpecialtyName() != null ? d.getSpecialtyName() : ""
                        ))
                        .toList();

        return ResponseEntity.ok(Map.of(
                "clinicId", clinic.getId(),
                "clinicName", clinic.getName(),
                "organizationId", org.getId(),
                "organizationName", org.getName(),
                "openToday", clinic.isOpenToday(),
                "closureReason", clinic.getClosureReason() != null ? clinic.getClosureReason() : "",
                "doctors", doctors
        ));
    }

    @GetMapping("/clinics/{clinicId}/doctors")
    public ResponseEntity<?> listDoctorsInClinic(@PathVariable Long clinicId,
                                                 @RequestParam(required = false) Long specialtyId) {
        var clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));
        var org = orgRepo.findById(clinic.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("المنشأة غير موجودة"));

        if (!org.isActive()
                || (org.getSubscriptionStatus() != SubscriptionStatus.ACTIVE
                    && org.getSubscriptionStatus() != SubscriptionStatus.TRIAL)) {
            return ResponseEntity.ok(List.of());
        }

        var assignments = assignmentRepo.findByClinicIdAndActiveTrue(clinicId);
        var doctorIds = assignments.stream().map(DoctorClinicAssignmentEntity::getDoctorId).toList();
        if (doctorIds.isEmpty()) return ResponseEntity.ok(List.of());

        var doctors = doctorRepo.findByUserIdIn(doctorIds).stream()
                .filter(d -> specialtyId == null || specialtyId.equals(d.getSpecialtyId()))
                .map(d -> {
                    var assign = assignments.stream()
                            .filter(a -> a.getDoctorId().equals(d.getUserId())).findFirst().orElse(null);

                    BigDecimal examFee = (assign != null && assign.getExaminationFee() != null)
                            ? assign.getExaminationFee()
                            : (d.getConsultationFee() != null ? d.getConsultationFee() : BigDecimal.valueOf(200));
                    BigDecimal followUpFee = (assign != null && assign.getFollowUpFee() != null)
                            ? assign.getFollowUpFee()
                            : (d.getFollowUpFee() != null ? d.getFollowUpFee() : BigDecimal.valueOf(100));

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("doctorId", d.getUserId());
                    m.put("fullName", d.getFullName());
                    m.put("specialtyName", d.getSpecialtyName());
                    m.put("consultationFee", examFee);
                    m.put("examinationFee", examFee);
                    m.put("followUpFee", followUpFee);
                    m.put("workingDays", assign != null ? assign.getWorkingDays() : clinic.getWorkingDays());
                    m.put("arrivalTime", assign != null ? assign.getArrivalTime() : "17:00");
                    m.put("departureTime", assign != null ? assign.getDepartureTime() : "22:00");
                    return m;
                }).toList();
        return ResponseEntity.ok(doctors);
    }
}