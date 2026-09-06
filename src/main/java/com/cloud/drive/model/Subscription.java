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

    /** ACTIVE or CANCELLED */
    @Column(nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String stripeCustomerId;
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}