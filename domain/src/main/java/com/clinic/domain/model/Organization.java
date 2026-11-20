package com.clinic.domain.model;

import com.clinic.domain.exception.SubscriptionException;
import com.clinic.domain.model.enums.OrganizationType;
import com.clinic.domain.model.enums.SubscriptionPlan;
import com.clinic.domain.model.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public class Organization {
    private final Long id;
    private String name;
    private String slug;
    private OrganizationType type;
    private SubscriptionPlan plan;
    private SubscriptionStatus subscriptionStatus;
    private boolean active;
    private LocalDateTime trialEndsAt;
    private LocalDateTime subscriptionEndsAt;
    private final LocalDateTime createdAt;

    public Organization(Long id, String name, String slug, OrganizationType type,
                        SubscriptionPlan plan, SubscriptionStatus subscriptionStatus,
                        LocalDateTime trialEndsAt, LocalDateTime subscriptionEndsAt,
                        LocalDateTime createdAt) {
        this(id, name, slug, type, plan, subscriptionStatus, true, trialEndsAt, subscriptionEndsAt, createdAt);
    }

    public Organization(Long id, String name, String slug, OrganizationType type,
                        SubscriptionPlan plan, SubscriptionStatus subscriptionStatus,
                        boolean active, LocalDateTime trialEndsAt, LocalDateTime subscriptionEndsAt,
                        LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.type = type;
        this.plan = plan;
        this.subscriptionStatus = subscriptionStatus;
        this.active = active;
        this.trialEndsAt = trialEndsAt;
        this.subscriptionEndsAt = subscriptionEndsAt;
        this.createdAt = createdAt;
    }

    public void ensureActiveSubscription() {
        if (!active) {
            throw new SubscriptionException("تم إيقاف حساب المنشأة من قِبل إدارة المنصة. يرجى التواصل مع الدعم.");
        }
        if (subscriptionStatus == SubscriptionStatus.SUSPENDED
                || subscriptionStatus == SubscriptionStatus.CANCELLED
                || subscriptionStatus == SubscriptionStatus.PAST_DUE) {
            throw new SubscriptionException("اشتراك المنشأة غير نشط. يرجى تجديد الاشتراك للمتابعة.");
        }
        if (subscriptionStatus == SubscriptionStatus.TRIAL
                && trialEndsAt != null && trialEndsAt.isBefore(LocalDateTime.now())) {
            throw new SubscriptionException("انتهت الفترة التجريبية. يرجى اختيار باقة للمتابعة.");
        }
        if (subscriptionStatus == SubscriptionStatus.ACTIVE
                && subscriptionEndsAt != null && subscriptionEndsAt.isBefore(LocalDateTime.now())) {
            throw new SubscriptionException("انتهت صلاحية الاشتراك الشهري. يرجى التجديد.");
        }
    }

    public void ensureCanAddClinic(long currentClinicCount) {
        ensureActiveSubscription();
        if (currentClinicCount >= plan.getMaxClinics()) {
            throw new SubscriptionException("وصلت للحد الأقصى للفروع في باقتك الحالية (" + plan.getMaxClinics() + ").");
        }
    }

    public void ensureCanAddDoctor(long currentDoctorCount) {
        ensureActiveSubscription();
        if (type == OrganizationType.SOLO_DOCTOR && currentDoctorCount >= 1) {
            throw new SubscriptionException("حساب الطبيب المستقل يدعم طبيباً واحداً فقط. يمكنك الترقية إلى مركز طبي.");
        }
        if (currentDoctorCount >= plan.getMaxDoctors()) {
            throw new SubscriptionException("وصلت للحد الأقصى للأطباء في باقتك الحالية (" + plan.getMaxDoctors() + ").");
        }
    }

    public void ensureCanBookThisMonth(long monthlyAppointmentsCount) {
        ensureActiveSubscription();
        if (monthlyAppointmentsCount >= plan.getMaxMonthlyAppointments()) {
            throw new SubscriptionException("تم استهلاك الحد الأقصى للحجوزات الشهرية في باقتك الحالية.");
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public OrganizationType getType() { return type; }
    public SubscriptionPlan getPlan() { return plan; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public boolean isActive() { return active; }
    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public LocalDateTime getSubscriptionEndsAt() { return subscriptionEndsAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}