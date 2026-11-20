package com.clinic.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "clinics")
public class ClinicEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private String name;

    private String address;
    private String city;
    private String phone;
    private String workingDays = "SATURDAY,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY";
    private String bookingOpenTime = "00:00";
    private String bookingCloseTime = "23:59";
    private String doctorArrivalTime = "17:00";
    private String timezone = "Africa/Cairo";
    private boolean openToday = true;
    private String closureReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWorkingDays() { return workingDays; }
    public void setWorkingDays(String workingDays) { this.workingDays = workingDays; }
    public String getBookingOpenTime() { return bookingOpenTime; }
    public void setBookingOpenTime(String bookingOpenTime) { this.bookingOpenTime = bookingOpenTime; }
    public String getBookingCloseTime() { return bookingCloseTime; }
    public void setBookingCloseTime(String bookingCloseTime) { this.bookingCloseTime = bookingCloseTime; }
    public String getDoctorArrivalTime() { return doctorArrivalTime; }
    public void setDoctorArrivalTime(String doctorArrivalTime) { this.doctorArrivalTime = doctorArrivalTime; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public boolean isOpenToday() { return openToday; }
    public void setOpenToday(boolean openToday) { this.openToday = openToday; }
    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }
}