package com.clinic.infrastructure.adapter.in.web;

import com.clinic.application.command.BookAppointmentCommand;
import com.clinic.application.port.in.BookAppointmentUseCase;
import com.clinic.application.port.in.CancelPatientAppointmentUseCase;
import com.clinic.application.port.out.AppointmentRepositoryPort;
import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.domain.model.enums.UserRole;
import com.clinic.domain.model.enums.VisitType;
import com.clinic.infrastructure.adapter.in.web.dto.AppointmentResponse;
import com.clinic.infrastructure.adapter.in.web.dto.BookAppointmentRequest;
import com.clinic.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final BookAppointmentUseCase bookUseCase;
    private final CancelPatientAppointmentUseCase cancelUseCase;
    private final AppointmentRepositoryPort appointmentRepositoryPort;
    private final ClinicStatusPort clinicStatusPort;

    public AppointmentController(BookAppointmentUseCase bookUseCase,
                                 CancelPatientAppointmentUseCase cancelUseCase,
                                 AppointmentRepositoryPort appointmentRepositoryPort,
                                 ClinicStatusPort clinicStatusPort) {
        this.bookUseCase = bookUseCase;
        this.cancelUseCase = cancelUseCase;
        this.appointmentRepositoryPort = appointmentRepositoryPort;
        this.clinicStatusPort = clinicStatusPort;
    }

    @PostMapping({"", "/book"})
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest req,
                                                    @AuthenticationPrincipal CustomUserDetails user) {
        Long patientId = user.getRole() == UserRole.PATIENT ? user.getUserId() : req.patientId();
        if (patientId == null) {
            throw new IllegalArgumentException("معرف المريض مطلوب");
        }
        LocalDateTime scheduledAt = req.scheduledAt() != null
                ? req.scheduledAt()
                : LocalDateTime.now(clinicStatusPort.getClinicZoneId(req.clinicId()));

        VisitType visitType = req.visitType() != null ? req.visitType() : VisitType.EXAMINATION;
        var cmd = new BookAppointmentCommand(patientId, req.doctorId(), req.clinicId(), scheduledAt, visitType);
        var result = bookUseCase.book(cmd);
        var response = new AppointmentResponse(
                result.appointmentId(),
                result.queueNumber() != null ? result.queueNumber() : 0,
                result.status().name(),
                result.scheduledAt(),
                result.visitType().name(),
                result.fee(),
                result.priority());
        return ResponseEntity.created(URI.create("/api/appointments/" + result.appointmentId())).body(response);
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> cancelByPatient(@PathVariable Long appointmentId,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        cancelUseCase.cancel(appointmentId, user.getUserId());
        return ResponseEntity.ok(Map.of("message", "تم إلغاء الحجز بنجاح"));
    }

    @GetMapping("/active/{patientId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getActiveAppointment(@PathVariable Long patientId,
                                                  @AuthenticationPrincipal CustomUserDetails user) {
        if (!user.getUserId().equals(patientId)) {
            throw new AccessDeniedException("غير مصرح لك بعرض حجوزات مريض آخر");
        }
        var active = appointmentRepositoryPort.findActiveByPatientId(patientId, LocalDate.now(ZoneId.of("Africa/Cairo")));
        if (active.isPresent()) {
            var a = active.get();
            return ResponseEntity.ok(Map.of(
                    "appointmentId", a.getId(),
                    "clinicId", a.getClinicId(),
                    "doctorId", a.getDoctorId(),
                    "queueNumber", a.getQueueNumber() != null ? a.getQueueNumber() : 0,
                    "status", a.getStatus().name(),
                    "visitType", a.getVisitType().name(),
                    "fee", a.getFee(),
                    "priority", a.isPriority()));
        }
        return ResponseEntity.noContent().build();
    }
}