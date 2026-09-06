package com.cloud.drive.dto.plan;

public class PlanResponse {
    private String name;
    private String slug;
    private long storageLimitBytes;
    private long maxFileSizeBytes;
    private int maxTeams;
    private int maxTeamMembers;
    private int aiQueriesPerMonth;
    private long rateLimitPerMinute;
    private int priceCents;
    private String currency;
    private String billingInterval;

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
}
