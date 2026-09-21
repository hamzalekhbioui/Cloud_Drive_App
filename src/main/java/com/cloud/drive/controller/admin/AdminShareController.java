package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.file.AdminShareDto;
import com.cloud.drive.service.admin.AdminFileService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shares")
public class AdminShareController {
    private final AdminFileService fileService;

    public AdminShareController(AdminFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public Page<AdminShareDto> list(@RequestParam(required = false) String owner,
                                    @RequestParam(required = false) Boolean revoked,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return fileService.listShares(owner, revoked, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/{shareId}/revoke")
    public AdminShareDto revoke(@PathVariable Long shareId) {
        return fileService.revokeShare(shareId);
    }
}
