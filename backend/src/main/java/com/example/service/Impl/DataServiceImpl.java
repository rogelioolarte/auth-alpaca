package com.example.service.Impl;

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
        Permission readPermission = new Permission();
        readPermission.setPermissionName("READ");
        Permission updatePermission = new Permission();
        updatePermission.setPermissionName("UPDATE");
        Permission deletePermission = new Permission();
        deletePermission.setPermissionName("DELETE");
        Permission createPermission = new Permission();
        createPermission.setPermissionName("CREATE");
        permissionService.saveAll(List.of(readPermission, updatePermission,
                createPermission, deletePermission));

        Role adminRole = new Role();
        adminRole.setRoleName("ADMIN");
        adminRole.setRoleDescription("es un admin");
        adminRole.setPermissions(Set.of(readPermission, updatePermission,
                deletePermission, createPermission));
        Role userRole = new Role();
        userRole.setRoleName("USER");
        userRole.setRoleDescription("es un user");
        userRole.setPermissions(Set.of(readPermission, createPermission));
        Role managerRole = new Role();
        managerRole.setRoleName("MANAGER");
        managerRole.setRoleDescription("es un manager");
        managerRole.setPermissions(Set.of(readPermission, updatePermission, createPermission));
        roleService.saveAll(List.of(adminRole, managerRole, userRole));

        User testGoogleUser = new User();
        testGoogleUser.setUsername("mcqueenrayo104@gmail.com");
        testGoogleUser.setPassword(passwordManager.encodePassword("123456789"));
        testGoogleUser.setRoles(Set.of(userRole));
        testGoogleUser.setEnabled(true);
        testGoogleUser.setAccountNoExpired(true);
        testGoogleUser.setAccountNoLocked(true);
        testGoogleUser.setCredentialNoExpired(true);
        User adminUser = new User();
        adminUser.setUsername("admin@admin.com");
        adminUser.setPassword(passwordManager.encodePassword("123456789"));
        adminUser.setRoles(Set.of(adminRole, userRole, managerRole));
        adminUser.setEnabled(true);
        adminUser.setAccountNoExpired(true);
        adminUser.setAccountNoLocked(true);
        adminUser.setCredentialNoExpired(true);
        User managerUser = new User();
        managerUser.setUsername("manager@manager.com");
        managerUser.setPassword(passwordManager.encodePassword("123456789"));
        managerUser.setRoles(Set.of(managerRole));
        managerUser.setEnabled(true);
        managerUser.setAccountNoExpired(true);
        managerUser.setAccountNoLocked(true);
        managerUser.setCredentialNoExpired(true);
        User userUser = new User();
        userUser.setUsername("user@user.com");
        userUser.setPassword(passwordManager.encodePassword("123456789"));
        userUser.setRoles(Set.of(userRole));
        userUser.setEnabled(true);
        userUser.setAccountNoExpired(true);
        userUser.setAccountNoLocked(true);
        userUser.setCredentialNoExpired(true);
        userService.saveAll(List.of(adminUser, managerUser, userUser, testGoogleUser));

        Profile adminProfile = new Profile();
        adminProfile.setFirstName("Admin");
        adminProfile.setLastName("Last");
        adminProfile.setAvatarUrl("https://foto.admin.com");
        adminProfile.setAddress("av admin 01");
        adminProfile.setUser(adminUser);
        Profile userProfile = new Profile();
        userProfile.setFirstName("User");
        userProfile.setLastName("Last");
        userProfile.setAvatarUrl("https://foto.user.com");
        userProfile.setAddress("av user 01");
        userProfile.setUser(userUser);
        Profile managerProfile = new Profile();
        managerProfile.setFirstName("Manager");
        managerProfile.setLastName("Last");
        managerProfile.setAvatarUrl("https://foto.invited.com");
        managerProfile.setAddress("av manager 01");
        managerProfile.setUser(managerUser);
        Profile testingGoogleProfile = new Profile();
        testingGoogleProfile.setFirstName("Testing google");
        testingGoogleProfile.setLastName("Last");
        testingGoogleProfile.setAvatarUrl("https://foto.testing.com");
        testingGoogleProfile.setAddress("av testing 01");
        testingGoogleProfile.setUser(testGoogleUser);
        profileService.saveAll(List.of(adminProfile, managerProfile,
                userProfile, testingGoogleProfile));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpHibernate() {
        roleService.findAll();
    }
}
