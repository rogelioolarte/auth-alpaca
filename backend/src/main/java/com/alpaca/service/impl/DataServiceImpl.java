package com.alpaca.service.impl;

import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.service.DataService;
import com.alpaca.service.IProfileService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Service responsible for seeding essential application data upon startup.
 *
 * <p>Implements {@link DataService} and initializes default permissions, roles, users, and
 * profiles. Listens for {@link ApplicationReadyEvent} to trigger post-startup actions, such as
 * warming up caches or validating system readiness.
 *
 * <p>This ensures the application is populated with baseline data when it starts in a clean state.
 *
 * @see DataService
 * @see ApplicationReadyEvent
 */
@Generated
@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final IRoleService roleService;
    private final IUserService userService;
    private final IProfileService profileService;

    @Value("${security.default-admin.email}")
    private String adminEmail;

    @Value("${security.default-admin.password}")
    private String adminPassword;

    /**
     * Provisions the default administrator user and profile if no user records exist yet.
     *
     * <p>This is a guarded startup operation — if any users are already present in the database the
     * method returns immediately to avoid overwriting existing data. If the admin password
     * configuration is empty or missing, a warning is logged and the method exits without creating
     * anything.
     *
     * <p>Marked {@link Transactional} to ensure atomicity of user + profile creation.
     */
    @Transactional
    @Override
    public void initializeAdminUser() {
        if (!userService.findAll().isEmpty()) {
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN USER NOT PROVISIONED: The ADMIN_PASSWORD variable is empty.");
            return;
        }

        log.info("Provisioning initial administrator user...");
        Role adminRole = roleService.findByRoleName("ADMIN");

        User adminUser =
                userService.save(
                        new User(adminEmail, adminPassword, new HashSet<>(Set.of(adminRole))));

        profileService.save(
                Profile.builder()
                        .firstName("Admin")
                        .lastName("Last")
                        .address("")
                        .avatarUrl("")
                        .user(adminUser)
                        .build());
    }

    /**
     * Seeds the database with default users, and profiles, only if no user records already exist.
     *
     * <p>Marked {@link Transactional} to ensure atomicity and rollback safety.
     */
    @Transactional
    @Override
    public void initializeData() {
        if (userService.findAll().size() > 1) {
            return;
        }
        String commonPass = "123456789";

        Role userRole = roleService.findByRoleName("USER");
        Role managerRole = roleService.findByRoleName("MANAGER");

        User managerUser =
                userService.save(
                        new User(
                                "manager@manager.com",
                                commonPass,
                                new HashSet<>(Set.of(managerRole))));
        User userUser =
                userService.save(
                        new User("user@user.com", commonPass, new HashSet<>(Set.of(userRole))));

        profileService.save(
                Profile.builder()
                        .firstName("User")
                        .lastName("Last")
                        .address("")
                        .avatarUrl("")
                        .user(userUser)
                        .build());
        profileService.save(
                Profile.builder()
                        .firstName("Manager")
                        .lastName("Last")
                        .address("")
                        .avatarUrl("")
                        .user(managerUser)
                        .build());
    }

    /**
     * Invoked after the application is fully initialized and ready to serve requests. Triggers a
     * base call to preload roles and logs system readiness.
     *
     * <p>Using {@link EventListener} with {@link ApplicationReadyEvent} ensures this runs only once
     * the Spring context is fully set up.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpHibernate() {
        roleService.findAll();
        log.info("System Ready!");
    }
}
