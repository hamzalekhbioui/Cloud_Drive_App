package com.cloud.drive.security.admin;

import com.cloud.drive.model.AdminUser;
import com.cloud.drive.repository.AdminUserRepository;
import com.cloud.drive.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates admin tokens on the {@code /api/admin/**} chain only.
 * Never consults the tenant {@code users} table.
 */
@Component
public class AdminJwtAuthFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private final AdminJwtUtil adminJwtUtil;
    private final AdminUserRepository adminUserRepository;
    private final JwtUtil tenantJwtUtil;

    public AdminJwtAuthFilter(AdminJwtUtil adminJwtUtil, AdminUserRepository adminUserRepository,
                              JwtUtil tenantJwtUtil) {
        this.adminJwtUtil = adminJwtUtil;
        this.adminUserRepository = adminUserRepository;
        this.tenantJwtUtil = tenantJwtUtil;
    }

    /**
     * This filter is a {@code @Component}, so Boot also auto-registers it for the whole
     * servlet context. Restrict it to the admin chain's paths so admin credentials are
     * never evaluated on tenant endpoints.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (tenantJwtUtil.isValid(token)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (adminJwtUtil.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<AdminUser> admin = adminUserRepository.findByEmail(adminJwtUtil.extractEmail(token));
            if (admin.isPresent() && AdminUser.STATUS_ACTIVE.equals(admin.get().getStatus())) {
                AdminPrincipal principal = AdminPrincipal.of(admin.get());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
