package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    /** Storage limits in bytes for each plan. */
    public static final long FREE_BYTES    = 5L   * 1024 * 1024 * 1024;   // 5 GB
    public static final long PRO_BYTES     = 50L  * 1024 * 1024 * 1024;   // 50 GB
    public static final long BUSINESS_BYTES = 1024L * 1024 * 1024 * 1024; // 1 TB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userEmail;

    /** FREE, PRO, or BUSINESS */
    @Column(nullable = false)
    private String plan = "FREE";

    /** ACTIVE or CANCELLED */
    @Column(nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String stripeCustomerId;
    private String stripeSubscriptionId;

    public Subscription() {}

    public long getPlanLimitBytes() {
        return switch (plan) {
            case "PRO"      -> PRO_BYTES;
            case "BUSINESS" -> BUSINESS_BYTES;
            default         -> FREE_BYTES;
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

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
}