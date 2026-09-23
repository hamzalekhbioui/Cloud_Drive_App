package com.cloud.drive.dto.admin.billing;

import java.time.LocalDateTime;

public class AdminPaymentDto {
    private Long id;
    private String userEmail;
    private Long subscriptionId;
    private String stripePaymentIntentId;
    private String stripeInvoiceId;
    private int amountCents;
    private String currency;
    private String status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String value) { this.userEmail = value; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long value) { this.subscriptionId = value; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String value) { this.stripePaymentIntentId = value; }
    public String getStripeInvoiceId() { return stripeInvoiceId; }
    public void setStripeInvoiceId(String value) { this.stripeInvoiceId = value; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int value) { this.amountCents = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
