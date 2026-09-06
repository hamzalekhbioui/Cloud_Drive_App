package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "storage_limit_bytes", nullable = false)
    private long storageLimitBytes;

    @Column(name = "max_file_size_bytes", nullable = false)
    private long maxFileSizeBytes;

    @Column(name = "max_teams", nullable = false)
    private int maxTeams;

    @Column(name = "max_team_members", nullable = false)
    private int maxTeamMembers;

    @Column(name = "ai_queries_per_month", nullable = false)
    private int aiQueriesPerMonth;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private long rateLimitPerMinute;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(name = "billing_interval", nullable = false)
    private String billingInterval = "MONTH";

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Plan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public long getStorageLimitBytes() { return storageLimitBytes; }
    public void setStorageLimitBytes(long storageLimitBytes) { this.storageLimitBytes = storageLimitBytes; }

    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

    public int getMaxTeams() { return maxTeams; }
    public void setMaxTeams(int maxTeams) { this.maxTeams = maxTeams; }

    public int getMaxTeamMembers() { return maxTeamMembers; }
    public void setMaxTeamMembers(int maxTeamMembers) { this.maxTeamMembers = maxTeamMembers; }

    public int getAiQueriesPerMonth() { return aiQueriesPerMonth; }
    public void setAiQueriesPerMonth(int aiQueriesPerMonth) { this.aiQueriesPerMonth = aiQueriesPerMonth; }

    public long getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(long rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBillingInterval() { return billingInterval; }
    public void setBillingInterval(String billingInterval) { this.billingInterval = billingInterval; }

    public String getStripePriceId() { return stripePriceId; }
    public void setStripePriceId(String stripePriceId) { this.stripePriceId = stripePriceId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
