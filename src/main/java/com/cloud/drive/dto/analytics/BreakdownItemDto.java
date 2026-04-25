package com.cloud.drive.dto.analytics;

public class BreakdownItemDto {
    private String category;
    private long size;
    private double percentage;
    private String color;

    public BreakdownItemDto(String category, long size, double percentage, String color) {
        this.category = category;
        this.size = size;
        this.percentage = percentage;
        this.color = color;
    }

    public String getCategory()   { return category; }
    public long getSize()         { return size; }
    public double getPercentage() { return percentage; }
    public String getColor()      { return color; }
}