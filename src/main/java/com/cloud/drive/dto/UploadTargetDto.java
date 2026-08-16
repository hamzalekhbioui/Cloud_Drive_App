package com.cloud.drive.dto;

/**
 * Response returned by POST /api/files/upload/begin.
 * Contains everything the client needs to PUT the file directly to Azure.
 */
public class UploadTargetDto {

    private Long uploadId;
    private String writeUrl;
    private String blobKey;
    private long ttlSec;

    public UploadTargetDto() {}

    public UploadTargetDto(Long uploadId, String writeUrl, String blobKey, long ttlSec) {
        this.uploadId = uploadId;
        this.writeUrl = writeUrl;
        this.blobKey = blobKey;
        this.ttlSec = ttlSec;
    }

    public Long getUploadId() { return uploadId; }
    public void setUploadId(Long uploadId) { this.uploadId = uploadId; }

    public String getWriteUrl() { return writeUrl; }
    public void setWriteUrl(String writeUrl) { this.writeUrl = writeUrl; }

    public String getBlobKey() { return blobKey; }
    public void setBlobKey(String blobKey) { this.blobKey = blobKey; }

    public long getTtlSec() { return ttlSec; }
    public void setTtlSec(long ttlSec) { this.ttlSec = ttlSec; }
}
