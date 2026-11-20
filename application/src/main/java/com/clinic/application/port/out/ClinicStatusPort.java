package com.clinic.application.port.out;

import com.clinic.domain.model.enums.VisitType;
import java.math.BigDecimal;
import java.time.ZoneId;

public interface ClinicStatusPort {
    boolean isClinicOpen(Long clinicId);
    String getClosureReason(Long clinicId);
    boolean isWithinBookingHours(Long clinicId);
    String getBookingClosedMessage(Long clinicId);
    ZoneId getClinicZoneId(Long clinicId);
    Long getOrganizationIdByClinicId(Long clinicId);
    boolean isDoctorAssignedToClinic(Long clinicId, Long doctorId);
    int getDoctorAvgConsultationMinutes(Long doctorId);
    String getDoctorArrivalTime(Long clinicId);
    long calculateEstimatedWaitMinutes(Long clinicId, Long doctorId, int waitingCount, boolean consultationStarted);
    void closeClinicToday(Long clinicId, String reason);
    BigDecimal resolveVisitFee(Long clinicId, Long doctorId, VisitType visitType);
}