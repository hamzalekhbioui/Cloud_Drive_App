package com.cloud.drive.dto.analytics;

public class InsightDto {
    /** One of: "info" | "warning" | "success" | "tip" */
    private String type;
    private String message;
    private String detail;

    public InsightDto(String type, String message, String detail) {
        this.type = type;
        this.message = message;
        this.detail = detail;
    }

    public String getType()    { return type; }
    public String getMessage() { return message; }
    public String getDetail()  { return detail; }
}