package com.cloud.drive.dto.billing;

public class BillingSessionResponse {
    private String id;
    private String url;

    public BillingSessionResponse() {}

    public BillingSessionResponse(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
}
