package com.clinic.domain.model;

public class Clinic {
    private final Long id;
    private final Long organizationId;
    private final String name;
    private final String address;
    private final String city;
    private final String workingDays;
    private final String bookingOpenTime;
    private final String bookingCloseTime;
    private boolean openToday;
    private String closureReason;

    public Clinic(Long id, Long organizationId, String name, String address, String city,
                  String workingDays, String bookingOpenTime, String bookingCloseTime,
                  boolean openToday, String closureReason) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.address = address;
        this.city = city;
        this.workingDays = workingDays;
        this.bookingOpenTime = bookingOpenTime;
        this.bookingCloseTime = bookingCloseTime;
        this.openToday = openToday;
        this.closureReason = openToday ? null : closureReason;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getWorkingDays() { return workingDays; }
    public String getBookingOpenTime() { return bookingOpenTime; }
    public String getBookingCloseTime() { return bookingCloseTime; }
    public boolean isOpenToday() { return openToday; }
    public String getClosureReason() { return closureReason; }
}