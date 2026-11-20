package com.clinic.infrastructure.adapter.in.web.dto;

import com.clinic.infrastructure.adapter.out.persistence.ClinicEntity;

public record ClinicDetailsResponse(
        Long id,
        Long organizationId,
        String name,
        String address,
        String city,
        String phone,
        String workingDays,
        String bookingOpenTime,
        String bookingCloseTime,
        String doctorArrivalTime,
        String timezone,
        boolean openToday,
        String closureReason,
        boolean withinBookingHours,
        boolean canBookNow,
        String bookingStatusMessage
) {
    public static ClinicDetailsResponse from(ClinicEntity c,
                                             boolean clinicOpen,
                                             boolean withinBookingHours,
                                             String bookingStatusMessage) {
        boolean canBook = clinicOpen && withinBookingHours;
        return new ClinicDetailsResponse(
                c.getId(),
                c.getOrganizationId(),
                c.getName(),
                c.getAddress(),
                c.getCity(),
                c.getPhone(),
                c.getWorkingDays(),
                c.getBookingOpenTime(),
                c.getBookingCloseTime(),
                c.getDoctorArrivalTime(),
                c.getTimezone(),
                c.isOpenToday(),
                c.getClosureReason(),
                withinBookingHours,
                canBook,
                bookingStatusMessage
        );
    }
}