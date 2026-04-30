package com.cloud.drive.controller;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.share.CreateShareRequest;
import com.cloud.drive.dto.share.ShareResponse;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.service.BlobStorageService;
import com.cloud.drive.service.ShareService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/shares")
public class ShareController {

    private final ShareService shareService;
    private final BlobStorageService blobStorage;

    public ShareController(ShareService shareService, BlobStorageService blobStorage) {
        this.shareService = shareService;
        this.blobStorage = blobStorage;
    }

    @PostMapping("/files/{fileId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareResponse createShare(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateShareRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return shareService.createShare(fileId, ud.getUsername(), req);
    }

    @GetMapping("/files/{fileId}")
    public List<ShareResponse> getSharesForFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails ud) {
        return shareService.getSharesForFile(fileId, ud.getUsername());
    }

    @DeleteMapping("/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeShare(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserDetails ud) {
        shareService.revokeShare(shareId, ud.getUsername());
    }

    @GetMapping("/shared-with-me")
    public List<ShareResponse> sharedWithMe(@AuthenticationPrincipal UserDetails ud) {
        return shareService.getFilesSharedWithMe(ud.getUsername());
    }

    /** Public endpoint — no authentication required. */
    @GetMapping("/public/{token}")
    public FileResponseDto resolvePublicLink(@PathVariable String token) {
        return shareService.resolvePublicToken(token);
    }

    /** Public stream endpoint — no authentication required. */
    @GetMapping("/public/{token}/stream")
    public void streamPublicLink(@PathVariable String token, HttpServletResponse response) throws IOException {
        FileEntity file = shareService.resolveTokenForStream(token);
        String ct = file.getType() != null ? file.getType() : "application/octet-stream";
        response.setContentType(ct);
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getOriginalFileName() + "\"");
        blobStorage.streamToOutput(file.getBlobFileName(), response.getOutputStream());
    }
}