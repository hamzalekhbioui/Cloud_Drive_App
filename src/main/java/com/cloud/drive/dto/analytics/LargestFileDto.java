package com.cloud.drive.dto.analytics;

public class LargestFileDto {
    private Long id;
    private String name;
    private long size;
    private String type;
    private String createdAt;

    public LargestFileDto(Long id, String name, long size, String type, String createdAt) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
        this.createdAt = createdAt;
    }

    public Long getId()         { return id; }
    public String getName()     { return name; }
    public long getSize()       { return size; }
    public String getType()     { return type; }
    public String getCreatedAt(){ return createdAt; }
}
