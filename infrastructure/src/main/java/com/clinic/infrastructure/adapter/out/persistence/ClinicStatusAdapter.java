package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.application.port.out.ClinicStatusPort;
import com.clinic.domain.model.enums.VisitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Component
public class ClinicStatusAdapter implements ClinicStatusPort {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Cairo");

    private final ClinicJpaRepository clinicRepo;
    private final DoctorClinicAssignmentJpaRepository assignmentRepo;
    private final DoctorProfileJpaRepository doctorRepo;
    private final OrganizationJpaRepository orgRepo;

    public ClinicStatusAdapter(ClinicJpaRepository clinicRepo,
                               DoctorClinicAssignmentJpaRepository assignmentRepo,
                               DoctorProfileJpaRepository doctorRepo,
                               OrganizationJpaRepository orgRepo) {
        this.clinicRepo = clinicRepo;
        this.assignmentRepo = assignmentRepo;
        this.doctorRepo = doctorRepo;
        this.orgRepo = orgRepo;
    }

    @Override
    public boolean isClinicOpen(Long clinicId) {
        return clinicRepo.findById(clinicId).map(clinic -> {
            boolean orgActive = orgRepo.findById(clinic.getOrganizationId())
                    .map(OrganizationEntity::isActive)
                    .orElse(false);
            if (!orgActive || !clinic.isOpenToday()) return false;

            ZoneId zone = resolveZone(clinic);
            String today = LocalDate.now(zone).getDayOfWeek().name();
            String workingDays = clinic.getWorkingDays();
            return workingDays == null || workingDays.isBlank()
                    || Arrays.asList(workingDays.split(",")).contains(today);
        }).orElse(false);
    }

    @Override
    public String getClosureReason(Long clinicId) {
        return clinicRepo.findById(clinicId).map(clinic -> {
            boolean orgActive = orgRepo.findById(clinic.getOrganizationId())
                    .map(OrganizationEntity::isActive)
                    .orElse(false);
            if (!orgActive) {
                return "تم إيقاف هذه المنشأة مؤقتاً";
            }
            if (!clinic.isOpenToday()) {
                return (clinic.getClosureReason() != null && !clinic.getClosureReason().isBlank())
                        ? clinic.getClosureReason()
                        : "مغلق من قبل الإدارة";
            }
            ZoneId zone = resolveZone(clinic);
            String today = LocalDate.now(zone).getDayOfWeek().name();
            if (clinic.getWorkingDays() != null && !clinic.getWorkingDays().isBlank()
                    && !Arrays.asList(clinic.getWorkingDays().split(",")).contains(today)) {
                return "الفرع لا يعمل في هذا اليوم";
            }
            return "";
        }).orElse("الفرع غير موجود");
    }

    @Override
    public boolean isWithinBookingHours(Long clinicId) {
        return clinicRepo.findById(clinicId).map(clinic -> {
            LocalTime open = parseTime(clinic.getBookingOpenTime(), LocalTime.MIN);
            LocalTime close = parseTime(clinic.getBookingCloseTime(), LocalTime.of(23, 59));
            LocalTime now = LocalTime.now(resolveZone(clinic)).truncatedTo(ChronoUnit.MINUTES);

            if (!open.isAfter(close)) {
                return !now.isBefore(open) && !now.isAfter(close);
            } else {
                return !now.isBefore(open) || !now.isAfter(close);
            }
        }).orElse(false);
    }

    @Override
    public String getBookingClosedMessage(Long clinicId) {
        return clinicRepo.findById(clinicId).map(clinic -> {
            LocalTime open = parseTime(clinic.getBookingOpenTime(), LocalTime.MIN);
            LocalTime close = parseTime(clinic.getBookingCloseTime(), LocalTime.of(23, 59));
            LocalTime now = LocalTime.now(resolveZone(clinic)).truncatedTo(ChronoUnit.MINUTES);

            String openAr = formatArabicTime(open);
            String closeAr = formatArabicTime(close);

            if (!open.isAfter(close) && now.isBefore(open)) {
                return "لم يبدأ وقت استقبال الحجوزات بعد. مواعيد الحجز المتاحة للفرع من " + openAr + " إلى " + closeAr + ".";
            }
            return "انتهى وقت استقبال الحجوزات اليوم. مواعيد الحجز المتاحة للفرع من " + openAr + " إلى " + closeAr + ".";
        }).orElse("الفرع غير موجود");
    }

    @Override
    public ZoneId getClinicZoneId(Long clinicId) {
        return clinicRepo.findById(clinicId).map(this::resolveZone).orElse(DEFAULT_ZONE);
    }

    @Override
    public Long getOrganizationIdByClinicId(Long clinicId) {
        return clinicRepo.findById(clinicId)
                .map(ClinicEntity::getOrganizationId)
                .orElseThrow(() -> new IllegalArgumentException("الفرع غير موجود"));
    }

    @Override
    public boolean isDoctorAssignedToClinic(Long clinicId, Long doctorId) {
        return assignmentRepo.findByClinicIdAndDoctorId(clinicId, doctorId)
                .map(DoctorClinicAssignmentEntity::isActive)
                .orElse(false);
    }

    @Override
    public int getDoctorAvgConsultationMinutes(Long doctorId) {
        return doctorRepo.findByUserId(doctorId)
                .map(DoctorProfileEntity::getAvgConsultationMinutes)
                .orElse(10);
    }

    @Override
    public String getDoctorArrivalTime(Long clinicId) {
        return clinicRepo.findById(clinicId)
                .map(c -> (c.getDoctorArrivalTime() != null && !c.getDoctorArrivalTime().isBlank())
                        ? c.getDoctorArrivalTime()
                        : "17:00")
                .orElse("17:00");
    }

    @Override
    public long calculateEstimatedWaitMinutes(Long clinicId, Long doctorId, int waitingCount, boolean consultationStarted) {
        int avgMins = getDoctorAvgConsultationMinutes(doctorId);
        long queueWaitMinutes = (long) waitingCount * avgMins;

        if (consultationStarted) {
            return queueWaitMinutes;
        }

        long minutesUntilArrival = clinicRepo.findById(clinicId).map(clinic -> {
            LocalTime arrival = parseTime(clinic.getDoctorArrivalTime(), null);
            if (arrival == null) return 0L;

            ZoneId zone = resolveZone(clinic);
            LocalTime now = LocalTime.now(zone).truncatedTo(ChronoUnit.MINUTES);
            LocalTime open = parseTime(clinic.getBookingOpenTime(), LocalTime.MIN);

            if (!open.isAfter(arrival)) {
                return now.isBefore(arrival) ? Duration.between(now, arrival).toMinutes() : 0L;
            } else {
                if (!now.isBefore(open)) {
                    return Duration.between(now, LocalTime.of(23, 59)).toMinutes() + 1
                            + Duration.between(LocalTime.MIN, arrival).toMinutes();
                } else if (now.isBefore(arrival)) {
                    return Duration.between(now, arrival).toMinutes();
                }
                return 0L;
            }
        }).orElse(0L);

        return minutesUntilArrival + queueWaitMinutes;
    }

    @Override
    public void closeClinicToday(Long clinicId, String reason) {
        clinicRepo.findById(clinicId).ifPresent(clinic -> {
            clinic.setOpenToday(false);
            clinic.setClosureReason(reason);
            clinicRepo.save(clinic);
        });
    }

    @Override
    public BigDecimal resolveVisitFee(Long clinicId, Long doctorId, VisitType visitType) {
        VisitType type = visitType != null ? visitType : VisitType.EXAMINATION;
        var assignOpt = assignmentRepo.findByClinicIdAndDoctorId(clinicId, doctorId);
        if (assignOpt.isPresent()) {
            var a = assignOpt.get();
            if (type == VisitType.EXAMINATION && a.getExaminationFee() != null) {
                return a.getExaminationFee();
            }
            if (type == VisitType.CONSULTATION && a.getFollowUpFee() != null) {
                return a.getFollowUpFee();
            }
        }
        return doctorRepo.findByUserId(doctorId).map(dp -> {
            if (type == VisitType.CONSULTATION) {
                return dp.getFollowUpFee() != null ? dp.getFollowUpFee() : BigDecimal.valueOf(100);
            }
            return dp.getConsultationFee() != null ? dp.getConsultationFee() : BigDecimal.valueOf(200);
        }).orElse(BigDecimal.ZERO);
    }

    private ZoneId resolveZone(ClinicEntity clinic) {
        try {
            if (clinic.getTimezone() != null && !clinic.getTimezone().isBlank()) {
                return ZoneId.of(clinic.getTimezone());
            }
        } catch (Exception ignored) {}
        return DEFAULT_ZONE;
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        try {
            if (value != null && !value.isBlank()) {
                return LocalTime.parse(value.length() > 5 ? value.substring(0, 5) : value);
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private String formatArabicTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        String period = hour >= 12 ? "مساءً" : "صباحاً";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format("%02d:%02d %s", hour12, minute, period);
    }
}