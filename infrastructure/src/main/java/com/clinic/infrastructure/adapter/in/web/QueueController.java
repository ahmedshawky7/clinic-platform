package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.port.in.*;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.application.result.CallNextResult;
import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.QueueSnapshot;
import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.BookingSource;
import com.clinic.domain.model.enums.VisitType;
import com.clinic.infrastructure.adapter.out.persistence.ClinicEntity;
import com.clinic.infrastructure.adapter.out.persistence.ClinicJpaRepository;
import com.clinic.infrastructure.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

    private final GetLiveQueueUseCase getLiveQueueUseCase;
    private final CallNextPatientUseCase callNextUseCase;
    private final SkipPatientUseCase skipUseCase;
    private final RequeuePatientUseCase requeueUseCase;
    private final GetSkippedPatientsUseCase getSkippedUseCase;
    private final AddWalkInPatientUseCase walkInUseCase;
    private final CancelTodayQueueUseCase cancelTodayQueueUseCase;
    private final AppointmentRepositoryPort appointmentRepo;
    private final ClinicStatusPort clinicStatusPort;
    private final UserQueryPort userQueryPort;
    private final ClinicJpaRepository clinicRepo;

    public QueueController(GetLiveQueueUseCase getLiveQueueUseCase,
                           CallNextPatientUseCase callNextUseCase,
                           SkipPatientUseCase skipUseCase,
                           RequeuePatientUseCase requeueUseCase,
                           GetSkippedPatientsUseCase getSkippedUseCase,
                           AddWalkInPatientUseCase walkInUseCase,
                           CancelTodayQueueUseCase cancelTodayQueueUseCase,
                           AppointmentRepositoryPort appointmentRepo,
                           ClinicStatusPort clinicStatusPort,
                           UserQueryPort userQueryPort,
                           ClinicJpaRepository clinicRepo) {
        this.getLiveQueueUseCase = getLiveQueueUseCase;
        this.callNextUseCase = callNextUseCase;
        this.skipUseCase = skipUseCase;
        this.requeueUseCase = requeueUseCase;
        this.getSkippedUseCase = getSkippedUseCase;
        this.walkInUseCase = walkInUseCase;
        this.cancelTodayQueueUseCase = cancelTodayQueueUseCase;
        this.appointmentRepo = appointmentRepo;
        this.clinicStatusPort = clinicStatusPort;
        this.userQueryPort = userQueryPort;
        this.clinicRepo = clinicRepo;
    }

    @GetMapping("/{clinicId}")
    public QueueSnapshot getSnapshot(@PathVariable Long clinicId, @RequestParam Long doctorId) {
        return getLiveQueueUseCase.getSnapshot(clinicId, doctorId);
    }

    @GetMapping("/{clinicId}/waiting-list")
    public List<Map<String, Object>> getWaitingList(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                    @PathVariable Long clinicId,
                                                    @RequestParam Long doctorId) {
        verifyClinicAccess(currentUser, clinicId);
        LocalDate today = LocalDate.now(clinicStatusPort.getClinicZoneId(clinicId));
        return appointmentRepo.findWaiting(clinicId, doctorId, today).stream()
                .map(a -> Map.<String, Object>of(
                        "appointmentId", a.getId(),
                        "patientId", a.getPatientId(),
                        "queueNumber", a.getQueueNumber(),
                        "patientName", userQueryPort.getPatientNameById(a.getPatientId()),
                        "priority", a.isPriority(),
                        "visitType", a.getVisitType().name(),
                        "fee", a.getFee()
                )).toList();
    }

    @GetMapping("/{clinicId}/skipped")
    public List<GetSkippedPatientsUseCase.SkippedPatientDto> getSkipped(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                        @PathVariable Long clinicId,
                                                                        @RequestParam Long doctorId) {
        verifyClinicAccess(currentUser, clinicId);
        return getSkippedUseCase.getSkippedPatients(clinicId, doctorId);
    }

    @PostMapping("/{clinicId}/call-next")
    public CallNextResult callNext(@AuthenticationPrincipal CustomUserDetails currentUser,
                                   @PathVariable Long clinicId,
                                   @RequestParam Long doctorId) {
        verifyClinicAccess(currentUser, clinicId);
        return callNextUseCase.callNext(clinicId, doctorId);
    }

    @PostMapping("/{clinicId}/skip-current")
    public void skipCurrent(@AuthenticationPrincipal CustomUserDetails currentUser,
                            @PathVariable Long clinicId,
                            @RequestParam Long doctorId) {
        verifyClinicAccess(currentUser, clinicId);
        skipUseCase.skipCurrent(clinicId, doctorId);
    }

    @PostMapping("/{clinicId}/requeue/{appointmentId}")
    public void requeue(@AuthenticationPrincipal CustomUserDetails currentUser,
                        @PathVariable Long clinicId,
                        @PathVariable Long appointmentId,
                        @RequestParam Long doctorId) {
        verifyClinicAccess(currentUser, clinicId);
        requeueUseCase.requeue(appointmentId, clinicId, doctorId);
    }

    @PostMapping("/{clinicId}/walk-in")
    public void addWalkIn(@AuthenticationPrincipal CustomUserDetails currentUser,
                          @PathVariable Long clinicId,
                          @RequestBody Map<String, Object> body) {
        verifyClinicAccess(currentUser, clinicId);
        Long doctorId = Long.valueOf(String.valueOf(body.get("doctorId")));
        Long patientId = (body.get("patientId") != null && !String.valueOf(body.get("patientId")).isBlank())
                ? Long.valueOf(String.valueOf(body.get("patientId"))) : null;
        String fullName = body.get("fullName") != null ? String.valueOf(body.get("fullName")) : null;
        String phone = body.get("phoneNumber") != null ? String.valueOf(body.get("phoneNumber")) : null;
        LocalDate dob = (body.get("dateOfBirth") != null && !String.valueOf(body.get("dateOfBirth")).isBlank())
                ? LocalDate.parse(String.valueOf(body.get("dateOfBirth"))) : null;

        VisitType visitType = VisitType.EXAMINATION;
        if (body.get("visitType") != null && !String.valueOf(body.get("visitType")).isBlank()) {
            visitType = VisitType.valueOf(String.valueOf(body.get("visitType")).toUpperCase());
        }
        boolean priority = Boolean.parseBoolean(String.valueOf(body.getOrDefault("priority", "false")));

        walkInUseCase.addWalkIn(patientId, fullName, dob, phone, clinicId, doctorId, visitType, priority);
    }

    @PostMapping("/{clinicId}/cancel-today")
    public CancelTodayQueueUseCase.CancelResult cancelToday(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                            @PathVariable Long clinicId,
                                                            @RequestParam(name = "doctorId", required = false) Long queryDoctorId,
                                                            @RequestBody(required = false) Map<String, Object> body) {
        verifyClinicAccess(currentUser, clinicId);
        Long doctorId = queryDoctorId;
        if (doctorId == null && body != null && body.get("doctorId") != null
                && !String.valueOf(body.get("doctorId")).isBlank()) {
            doctorId = Long.valueOf(String.valueOf(body.get("doctorId")));
        }
        String reason = (body != null && body.get("reason") != null) ? String.valueOf(body.get("reason")) : null;
        boolean closeClinic = body == null || body.get("closeClinic") == null || Boolean.TRUE.equals(body.get("closeClinic"));
        return cancelTodayQueueUseCase.cancelTodayQueue(clinicId, doctorId, reason, closeClinic);
    }

    @GetMapping("/{clinicId}/analytics")
    public Map<String, Object> analytics(@AuthenticationPrincipal CustomUserDetails currentUser,
                                         @PathVariable Long clinicId,
                                         @RequestParam Long doctorId) {
        verifyClinicAccess(currentUser, clinicId);
        ZoneId zone = clinicStatusPort.getClinicZoneId(clinicId);
        LocalDate today = LocalDate.now(zone);

        long totalPatients = userQueryPort.countPatientsByOrganization(currentUser.getOrganizationId());
        long todayAppointments = appointmentRepo.countByClinicAndDate(clinicId, today);
        int waitingCount = appointmentRepo.countWaiting(clinicId, doctorId, today);
        long onlineCount = appointmentRepo.countByClinicAndDateAndSource(clinicId, today, BookingSource.ONLINE);
        long walkInCount = appointmentRepo.countByClinicAndDateAndSource(clinicId, today, BookingSource.WALK_IN);

        List<Appointment> todayList = appointmentRepo.findByClinicAndDate(clinicId, today);
        BigDecimal todayRevenue = BigDecimal.ZERO;
        BigDecimal completedRevenue = BigDecimal.ZERO;
        BigDecimal examinationRevenue = BigDecimal.ZERO;
        BigDecimal consultationRevenue = BigDecimal.ZERO;
        long examinationCount = 0;
        long consultationCount = 0;

        for (Appointment a : todayList) {
            if (a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.NO_SHOW) {
                continue;
            }
            BigDecimal fee = a.getFee() != null ? a.getFee() : BigDecimal.ZERO;
            todayRevenue = todayRevenue.add(fee);
            if (a.getStatus() == AppointmentStatus.COMPLETED) {
                completedRevenue = completedRevenue.add(fee);
            }
            if (a.getVisitType() == VisitType.CONSULTATION) {
                consultationCount++;
                consultationRevenue = consultationRevenue.add(fee);
            } else {
                examinationCount++;
                examinationRevenue = examinationRevenue.add(fee);
            }
        }

        LocalDate start = today.minusDays(6);
        List<Object[]> dailyRows = appointmentRepo.countDailyByClinicBetween(clinicId, start, today);
        Map<LocalDate, Long> countByDay = new HashMap<>();
        for (Object[] r : dailyRows) {
            countByDay.put((LocalDate) r[0], ((Number) r[1]).longValue());
        }
        List<Map<String, Object>> weekly = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            LocalDate d = start.plusDays(i);
            weekly.add(Map.of("day", d.toString(), "appointments", countByDay.getOrDefault(d, 0L)));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalPatients", totalPatients);
        out.put("todayAppointments", todayAppointments);
        out.put("waitingCount", waitingCount);
        out.put("onlineCount", onlineCount);
        out.put("walkInCount", walkInCount);
        out.put("todayRevenue", todayRevenue);
        out.put("completedRevenue", completedRevenue);
        out.put("examinationCount", examinationCount);
        out.put("consultationCount", consultationCount);
        out.put("examinationRevenue", examinationRevenue);
        out.put("consultationRevenue", consultationRevenue);
        out.put("weekly", weekly);
        return out;
    }

    private void verifyClinicAccess(CustomUserDetails currentUser, Long clinicId) {
        ClinicEntity clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));
        if (!clinic.getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new AccessDeniedException("غير مصرح بالوصول لهذا الفرع");
        }
    }
}