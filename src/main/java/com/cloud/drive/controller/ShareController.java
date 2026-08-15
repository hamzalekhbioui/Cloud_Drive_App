package com.cloud.drive.controller;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.share.CreateShareRequest;
import com.cloud.drive.dto.share.SharedFileResponse;
import com.cloud.drive.dto.share.ShareResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.security.FilenamePolicy;
import com.cloud.drive.service.BlobStorageService;
import com.cloud.drive.service.ShareService;
import com.cloud.drive.util.MimePolicy;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping
public class ShareController {

    private final ShareService shareService;
    private final BlobStorageService blobStorage;

    public ShareController(ShareService shareService, BlobStorageService blobStorage) {
        this.shareService = shareService;
        this.blobStorage = blobStorage;
    }

    @PostMapping("/api/documents/{fileId}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareResponse createShare(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateShareRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return shareService.createShare(fileId, ud.getUsername(), req);
    }

    @GetMapping("/api/documents/{fileId}/shares")
    public List<ShareResponse> getSharesForFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails ud) {
        return shareService.getActiveSharesForFile(fileId, ud.getUsername());
    }

    @DeleteMapping("/api/documents/{fileId}/shares")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeShare(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails ud) {
        // Find the active share for this file and revoke it
        List<ShareResponse> active = shareService.getActiveSharesForFile(fileId, ud.getUsername());
        if (active.isEmpty()) {
            throw new ApiException("No active share found for this document", HttpStatus.NOT_FOUND);
        }
        // Assuming one active share as per requirements simplicity preference
        shareService.revokeShare(active.get(0).getId(), ud.getUsername());
    }

    /**
     * Returns files shared with the authenticated user.
     * Uses {@link SharedFileResponse} — intentionally omits share tokens
     * so recipients cannot bypass permission checks via the public stream endpoint.
     */
    @GetMapping("/api/shares/shared-with-me")
    public List<SharedFileResponse> sharedWithMe(@AuthenticationPrincipal UserDetails ud) {
        return shareService.getFilesSharedWithMe(ud.getUsername());
    }

    /** Public endpoint — no authentication required. */
    @GetMapping("/public/{token}")
    public FileResponseDto resolvePublicLink(@PathVariable String token) {
        return shareService.resolvePublicToken(token);
    }

    /**
     * Public stream endpoint — no authentication required.
     *
     * <p>Enforces the share's permission level:
     * <ul>
     *   <li>{@code VIEW} — only inline rendering; download is forbidden</li>
     *   <li>{@code DOWNLOAD} — both inline and download are allowed</li>
     * </ul>
     *
     * @param token    the share token
     * @param download if {@code true}, forces attachment disposition (requires DOWNLOAD permission)
     */
    @GetMapping("/public/{token}/stream")
    public void streamPublicLink(
            @PathVariable String token,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletResponse response) throws IOException {

        FileShare share = shareService.resolveTokenForStream(token);

        // Enforce VIEW vs DOWNLOAD permission
        if (download && "VIEW".equals(share.getPermission())) {
            throw new ApiException("Download not permitted for this link", HttpStatus.FORBIDDEN);
        }

        FileEntity file = shareService.fileFor(share);
        String contentType = file.getType() != null ? file.getType() : "application/octet-stream";
        response.setContentType(contentType);

        // Determine disposition based on permission + request
        String disposition;
        if (download) {
            disposition = "attachment";
        } else if (MimePolicy.shouldInline(file.getType())) {
            disposition = "inline";
        } else {
            // Non-inlineable type on a VIEW share → still serve it, but as attachment
            // (the file is not renderable inline anyway)
            if ("VIEW".equals(share.getPermission())) {
                throw new ApiException("Download not permitted for this link", HttpStatus.FORBIDDEN);
            }
            disposition = "attachment";
        }

        response.setHeader("Content-Disposition",
                disposition + "; filename=\"" + FilenamePolicy.encodeFilename(file.getOriginalFileName()) + "\"");
        blobStorage.streamToOutput(file.getBlobFileName(), response.getOutputStream());
    }
}