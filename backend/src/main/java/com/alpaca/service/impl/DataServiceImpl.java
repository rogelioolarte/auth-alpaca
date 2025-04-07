package com.alpaca.service.impl;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.DataService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

  private final IPermissionDAO permissionDAO;
  private final IRoleDAO roleDAO;
  private final IUserDAO userDAO;
  private final IProfileDAO profileDAO;
  private final PasswordManager passwordManager;

  @Transactional
  @Override
  public void initializeData() {
    Permission readPermission = new Permission("READ");
    Permission updatePermission = new Permission("UPDATE");
    Permission deletePermission = new Permission("DELETE");
    Permission createPermission = new Permission("CREATE");
    permissionDAO.saveAll(
        new ArrayList<>(
            List.of(readPermission, updatePermission, createPermission, deletePermission)));

    Role adminRole =
        new Role(
            "ADMIN",
            "It's an admin",
            new HashSet<>(
                Set.of(readPermission, updatePermission, deletePermission, createPermission)));
    Role userRole =
        new Role("USER", "It's an user", new HashSet<>(Set.of(readPermission, createPermission)));
    Role managerRole =
        new Role(
            "MANAGER",
            "it's a manager",
            new HashSet<>(Set.of(readPermission, updatePermission, createPermission)));
    roleDAO.saveAll(new ArrayList<>(List.of(adminRole, managerRole, userRole)));

    User testGoogleUser =
        new User(
            "mcqueenrayo104@gmail.com",
            passwordManager.encodePassword("123456789"),
            new HashSet<>(Set.of(userRole)));
    User adminUser =
        new User(
            "admin@admin.com",
            passwordManager.encodePassword("123456789"),
            new HashSet<>(Set.of(adminRole)));
    User managerUser =
        new User(
            "manager@manager.com",
            passwordManager.encodePassword("123456789"),
            new HashSet<>(Set.of(managerRole)));
    User userUser =
        new User(
            "user@user.com",
            passwordManager.encodePassword("123456789"),
            new HashSet<>(Set.of(userRole)));
    userDAO.saveAll(new ArrayList<>(List.of(adminUser, managerUser, userUser, testGoogleUser)));

    Profile adminProfile =
        new Profile("Admin", "Last", "https://foto.admin.com", "av admin 01", adminUser);
    Profile userProfile =
        new Profile("User", "Last", "https://foto.user.com", "av user 01", userUser);
    Profile managerProfile =
        new Profile("Manager", "Last", "https://foto.invited.com", "av manager 01", managerUser);
    Profile testingGoogleProfile =
        new Profile(
            "Testing google", "Last", "https://foto.testing.com", "av testing 01", testGoogleUser);
    profileDAO.saveAll(
        new ArrayList<>(List.of(adminProfile, managerProfile, userProfile, testingGoogleProfile)));
  }

  @EventListener(ApplicationReadyEvent.class)
  public void warmUpHibernate() {
    roleDAO.findAll();
    log.info("System Ready!");
  }
}
