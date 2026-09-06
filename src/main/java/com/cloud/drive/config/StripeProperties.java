package com.cloud.drive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String secretKey = "";
    private String publishableKey = "";
    private String webhookSecret = "";
    private String frontendSuccessUrl;
    private String frontendCancelUrl;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getPublishableKey() { return publishableKey; }
    public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getFrontendSuccessUrl() { return frontendSuccessUrl; }
    public void setFrontendSuccessUrl(String frontendSuccessUrl) { this.frontendSuccessUrl = frontendSuccessUrl; }

    public String getFrontendCancelUrl() { return frontendCancelUrl; }
    public void setFrontendCancelUrl(String frontendCancelUrl) { this.frontendCancelUrl = frontendCancelUrl; }
}
