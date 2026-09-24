package com.cloud.drive.controller;

import com.cloud.drive.dto.folder.*;
import com.cloud.drive.service.FolderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {
    private final FolderService folderService;
    public FolderController(FolderService folderService) { this.folderService = folderService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody CreateFolderRequest request,
                                 @AuthenticationPrincipal UserDetails user) {
        return folderService.create(user.getUsername(), request);
    }

    @GetMapping
    public List<FolderResponse> list(@RequestParam(required = false) Long teamId,
                                     @AuthenticationPrincipal UserDetails user) {
        return folderService.list(user.getUsername(), teamId);
    }

    @PatchMapping("/{folderId}")
    public FolderResponse update(@PathVariable Long folderId, @Valid @RequestBody UpdateFolderRequest request,
                                 @AuthenticationPrincipal UserDetails user) {
        return folderService.update(folderId, user.getUsername(), request);
    }
}
