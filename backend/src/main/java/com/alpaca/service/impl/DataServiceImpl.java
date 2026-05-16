package com.alpaca.service.impl;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.service.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final IPermissionService permissionService;
    private final IRoleService roleService;
    private final IUserService userService;
    private final IProfileService profileService;

    /**
     * Seeds the database with default permissions, roles, users, and profiles, only if no user
     * records already exist.
     *
     * <p>Marked {@link Transactional} to ensure atomicity and rollback safety.
     */
    @Transactional
    @Override
    public void initializeData() {
        if (!userService.findAll().isEmpty()) {
            return;
        }

        Permission readPermission = permissionService.save(new Permission("READ"));
        Permission updatePermission = permissionService.save(new Permission("UPDATE"));
        Permission deletePermission = permissionService.save(new Permission("DELETE"));
        Permission createPermission = permissionService.save(new Permission("CREATE"));
        String commonPass = "123456789";

        Role adminRole =
                roleService.save(
                        new Role(
                                "ADMIN",
                                "It's an admin",
                                new HashSet<>(
                                        Set.of(
                                                readPermission,
                                                updatePermission,
                                                deletePermission,
                                                createPermission))));
        Role userRole =
                roleService.save(
                        new Role(
                                "USER",
                                "It's a user",
                                new HashSet<>(Set.of(readPermission, createPermission))));
        Role managerRole =
                roleService.save(
                        new Role(
                                "MANAGER",
                                "It's a manager",
                                new HashSet<>(
                                        Set.of(
                                                readPermission,
                                                updatePermission,
                                                createPermission))));

        User testGoogleUser =
                userService.save(
                        new User(
                                "mcqueenrayo104@gmail.com",
                                commonPass,
                                new HashSet<>(Set.of(userRole))));
        User adminUser =
                userService.save(
                        new User("admin@admin.com", commonPass, new HashSet<>(Set.of(adminRole))));
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
                        .firstName("Admin")
                        .lastName("Last")
                        .address("av admin 01")
                        .avatarUrl("https://photo.admin.com")
                        .user(adminUser)
                        .build());
        profileService.save(
                Profile.builder()
                        .firstName("User")
                        .lastName("Last")
                        .address("av user 01")
                        .avatarUrl("https://photo.user.com")
                        .user(userUser)
                        .build());
        profileService.save(
                Profile.builder()
                        .firstName("Manager")
                        .lastName("Last")
                        .address("av manager 01")
                        .avatarUrl("https://photo.manager.com")
                        .user(managerUser)
                        .build());
        profileService.save(
                Profile.builder()
                        .firstName("Testing Profile")
                        .lastName("Last")
                        .address("av testing 01")
                        .avatarUrl("https://photo.testing.com")
                        .user(testGoogleUser)
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
