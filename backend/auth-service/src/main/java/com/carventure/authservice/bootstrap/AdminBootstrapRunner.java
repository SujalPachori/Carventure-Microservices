package com.carventure.authservice.bootstrap;

import com.carventure.authservice.entity.Role;
import com.carventure.authservice.entity.User;
import com.carventure.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Solves the bootstrap problem: role-granting is admin-only (RoleController),
 * so how does the FIRST admin get created?
 *
 * This runner creates FIRST admin user.
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_BOOTSTRAP_USERNAME:}")
    private String bootstrapUsername;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(bootstrapUsername) || !StringUtils.hasText(bootstrapPassword)) {
            return; // Not configured
        }

        boolean anAdminAlreadyExists = !findAllAdmins().isEmpty();
        if (anAdminAlreadyExists) {
            log.warn("ADMIN_BOOTSTRAP_USERNAME/PASSWORD are set but an admin already exists - "
                    + "skipping bootstrap. Unset these env vars; they no longer do anything.");
            return;
        }

        if (userRepository.existsByUsername(bootstrapUsername)) {
            log.error("Cannot bootstrap admin: username '{}' already exists as a non-admin user. "
                    + "Choose a different ADMIN_BOOTSTRAP_USERNAME.", bootstrapUsername);
            return;
        }

        User admin = User.builder()
                .username(bootstrapUsername)
                .password(passwordEncoder.encode(bootstrapPassword))
                .roles(Set.of(Role.ADMIN))
                .build();
        userRepository.save(admin);

        log.warn("Bootstrap admin account '{}' created. Unset ADMIN_BOOTSTRAP_USERNAME/PASSWORD now "
                + "and rotate this password via the database - it currently matches an environment "
                + "variable that may be visible in deployment tooling/logs.", bootstrapUsername);
    }

    private List<User> findAllAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(Role.ADMIN))
                .toList();
    }
}
