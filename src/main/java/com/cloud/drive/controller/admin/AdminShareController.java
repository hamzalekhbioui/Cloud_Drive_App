package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.file.AdminShareDto;
import com.cloud.drive.service.admin.AdminFileService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.cloud.drive.util.AdminPaging;
import jakarta.servlet.http.HttpServletRequest;

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
        return fileService.listShares(owner, revoked,
                AdminPaging.bounded(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/{shareId}/revoke")
    public AdminShareDto revoke(@PathVariable Long shareId, @AuthenticationPrincipal AdminPrincipal admin,
                                HttpServletRequest request) {
        return fileService.revokeShare(shareId, admin, request.getRemoteAddr());
    }
}
