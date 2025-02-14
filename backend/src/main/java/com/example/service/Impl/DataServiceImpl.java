package com.example.service.impl;

import com.example.entity.Permission;
import com.example.entity.Profile;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.security.manager.PasswordManager;
import com.example.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final IPermissionService permissionService;
    private final IRoleService roleService;
    private final IUserService userService;
    private final IProfileService profileService;
    private final PasswordManager passwordManager;

    @Transactional
    @Override
    public void initializeData() {
        Permission readPermission = new Permission("READ");
        Permission updatePermission = new Permission("UPDATE");
        Permission deletePermission = new Permission("DELETE");
        Permission createPermission = new Permission("CREATE");
        permissionService.saveAll(List.of(readPermission, updatePermission,
                createPermission, deletePermission));

        Role adminRole = new Role("ADMIN", "It's an admin",
                Set.of(readPermission, updatePermission, deletePermission, createPermission));
        Role userRole = new Role("USER", "It's an user",
                Set.of(readPermission, createPermission));
        Role managerRole = new Role("MANAGER", "it's a manager",
                Set.of(readPermission, updatePermission, createPermission));
        roleService.saveAll(List.of(adminRole, managerRole, userRole));

        User testGoogleUser = new User("mcqueenrayo104@gmail.com",
                passwordManager.encodePassword("123456789"), Set.of(userRole));
        User adminUser = new User("admin@admin.com",
                passwordManager.encodePassword("123456789"), Set.of(adminRole));
        User managerUser = new User("manager@manager.com",
                passwordManager.encodePassword("123456789"), Set.of(managerRole));
        User userUser = new User("user@user.com",
                passwordManager.encodePassword("123456789"), Set.of(userRole));
        userService.saveAll(List.of(adminUser, managerUser, userUser, testGoogleUser));

        Profile adminProfile = new Profile("Admin", "Last",
                "https://foto.admin.com", "av admin 01", adminUser);
        Profile userProfile = new Profile("User", "Last",
                "https://foto.user.com", "av user 01", userUser);
        Profile managerProfile = new Profile("Manager", "Last",
                "https://foto.invited.com", "av manager 01", managerUser);
        Profile testingGoogleProfile = new Profile("Testing google", "Last",
                "https://foto.testing.com", "av testing 01", testGoogleUser);
        profileService.saveAll(List.of(adminProfile, managerProfile,
                userProfile, testingGoogleProfile));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpHibernate() {
        roleService.findAll();
    }
}
