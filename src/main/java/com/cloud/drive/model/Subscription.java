package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userEmail;

    /** FREE, PRO, or BUSINESS */
    @Column(nullable = false)
    private String plan = "FREE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan planRecord;

    /** Lifecycle state mirrored from Stripe. */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Column(name = "billing_cycle", nullable = false)
    @Enumerated(EnumType.STRING)
    private BillingInterval billingInterval = BillingInterval.MONTH;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    /** Cumulative storage consumed — maintained atomically under pessimistic lock. */
    @Column(name = "used_bytes", nullable = false)
    private long usedBytes = 0;

    /** Optimistic-lock version for dirty-check safety. */
    @Version
    private Long version;

    public Subscription() {}

    /** Atomically reserves additional bytes (caller must hold the row lock). */
    public void reserve(long bytes) {
        this.usedBytes += bytes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public Plan getPlanRecord() { return planRecord; }
    public void setPlanRecord(Plan planRecord) {
        this.planRecord = planRecord;
        if (planRecord != null) this.plan = planRecord.getSlug();
    }

    public String getStatus() { return status.name(); }
    public SubscriptionStatus getStatusValue() { return status; }
    public void setStatus(String status) { this.status = SubscriptionStatus.valueOf(status.toUpperCase()); }
    public void setStatus(SubscriptionStatus status) {
        if (status == null) throw new IllegalArgumentException("Subscription status cannot be null");
        this.status = status;
    }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public BillingInterval getBillingInterval() { return billingInterval; }
    public void setBillingInterval(BillingInterval billingInterval) {
        if (billingInterval == null) throw new IllegalArgumentException("Billing interval cannot be null");
        this.billingInterval = billingInterval;
    }

    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }

    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public boolean isCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) { this.cancelAtPeriodEnd = cancelAtPeriodEnd; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public void applyStripeState(SubscriptionStatus nextStatus,
                                 LocalDateTime periodStart,
                                 LocalDateTime periodEnd,
                                 boolean cancelAtPeriodEnd) {
        if (nextStatus == null) throw new IllegalArgumentException("Subscription status cannot be null");
        if (periodEnd != null && periodStart != null && periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Subscription period end cannot precede its start");
        }
        this.status = nextStatus;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        if (nextStatus == SubscriptionStatus.CANCELLED) {
            this.endDate = periodEnd != null ? periodEnd : LocalDateTime.now();
        }
    }

    public void scheduleCancellation() {
        if (status == SubscriptionStatus.CANCELLED) return;
        this.cancelAtPeriodEnd = true;
    }

    public void undoScheduledCancellation() {
        if (status == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled subscription cannot be resumed");
        }
        this.cancelAtPeriodEnd = false;
    }

    public void cancelNow(LocalDateTime endedAt) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelAtPeriodEnd = false;
        this.endDate = endedAt != null ? endedAt : LocalDateTime.now();
    }

    public void renew(LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.INCOMPLETE_EXPIRED) {
            throw new IllegalStateException("An expired subscription cannot be renewed");
        }
        applyStripeState(SubscriptionStatus.ACTIVE, periodStart, periodEnd, false);
        this.endDate = null;
    }

    public void expireAtPeriodEnd() {
        if (status == SubscriptionStatus.CANCELLED) return;
        cancelNow(currentPeriodEnd);
    }
}