package com.cloud.drive.config;

import com.cloud.drive.model.AdminUser;
import com.cloud.drive.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${admin.bootstrap.enabled:true}") boolean enabled,
                          @Value("${admin.bootstrap.email:}") String email,
                          @Value("${admin.bootstrap.password:}") String password,
                          @Value("${admin.bootstrap.name:Administrator}") String name) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        if (!enabled || email.isBlank() || password.isBlank() || adminUserRepository.count() > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setEmail(email);
        admin.setName(name);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setStatus(AdminUser.STATUS_ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        adminUserRepository.save(admin);
    }
}
