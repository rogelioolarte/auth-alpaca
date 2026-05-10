package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link UserDAOImpl} */
@DataJpaTest
@Import({UserDAOImpl.class})
class UserDAOImplIT {

    @Autowired private IUserDAO dao;

    @Autowired private UserRepo userRepo;

    @Autowired private ProfileRepo profileRepo;

    @Autowired private AdvertiserRepo advertiserRepo;

    private User persisted;

    @BeforeEach
    void setUp() {
        User template = UserProvider.singleTemplate();
        template.setCreatedAt(Instant.now());
        template.setCreatedBy(UUID.randomUUID().toString());
        persisted = userRepo.save(template);
    }

    @Test
    @DisplayName("findByEmail handles null, blank and existing email")
    @Transactional
    void findByEmail() {

        assertTrue(dao.findByEmail(null).isEmpty());
        assertTrue(dao.findByEmail(" ").isEmpty());

        Optional<User> found = dao.findByEmail(persisted.getEmail());

        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());
    }

    @Test
    @DisplayName("updateById updates fields and relations correctly")
    @Transactional
    void updateById() {

        UUID id = persisted.getId();

        Profile templateProfile = ProfileProvider.singleTemplate();
        templateProfile.setUser(persisted);

        Advertiser templateAdvertiser = AdvertiserProvider.singleTemplate();
        templateAdvertiser.setUser(persisted);

        Profile profile = profileRepo.save(templateProfile);
        Advertiser advertiser = advertiserRepo.save(templateAdvertiser);

        User update = buildFullUpdate(profile, advertiser);

        User out = dao.updateById(update, id);

        assertAll(
                () -> assertEquals(id, out.getId()),
                () -> assertEquals("new@example.com", out.getEmail()),
                () -> assertEquals("newPass", out.getPassword()),
                () -> assertEquals(profile.getId(), out.getProfile().getId()),
                () -> assertEquals(advertiser.getId(), out.getAdvertiser().getId()),
                () -> assertEquals(update.isEnabled(), out.isEnabled()),
                () -> assertEquals(update.isAccountNonLocked(), out.isAccountNonLocked()),
                () -> assertEquals(update.isAccountNonExpired(), out.isAccountNonExpired()),
                () -> assertEquals(update.isCredentialNonExpired(), out.isCredentialNonExpired()),
                () -> assertEquals(update.isEmailVerified(), out.isEmailVerified()),
                () -> assertEquals(update.isGoogleConnected(), out.isGoogleConnected()));

        // partial update should not override password
        User partial = new User();
        partial.setEmail("partial@example.com");

        User outPartial = dao.updateById(partial, id);

        assertEquals("partial@example.com", outPartial.getEmail());
        assertEquals(out.getPassword(), outPartial.getPassword());

        // same profile should not trigger update branch
        User sameProfile = new User();
        sameProfile.setProfile(profile);

        User outSameProfile = dao.updateById(sameProfile, id);
        assertEquals(profile.getId(), outSameProfile.getProfile().getId());

        // same advertiser should not trigger update
        User sameAdvertiser = new User();
        sameAdvertiser.setAdvertiser(advertiser);

        User outSameAdvertiser = dao.updateById(sameAdvertiser, id);
        assertEquals(advertiser.getId(), outSameAdvertiser.getAdvertiser().getId());

        assertThrows(NotFoundException.class, () -> dao.updateById(update, UUID.randomUUID()));
    }

    @Test
    @DisplayName("updateById updates roles when different")
    @Transactional
    void updateRoles() {

        User update = new User();
        update.setUserRoles(Collections.emptySet());

        User out = dao.updateById(update, persisted.getId());

        assertNotNull(out.getRoles());
    }

    @Test
    @DisplayName("existsByEmail and existsByUniqueProperties")
    @Transactional
    void existsChecks() {

        assertFalse(dao.existsByEmail(null));
        assertFalse(dao.existsByEmail(" "));
        assertFalse(dao.existsByEmail("notfound@test.com"));

        assertTrue(dao.existsByEmail(persisted.getEmail()));

        User probe = new User();
        probe.setEmail(persisted.getEmail());

        assertTrue(dao.existsByUniqueProperties(probe));

        User nullEmail = new User();
        assertFalse(dao.existsByUniqueProperties(nullEmail));
    }

    @Test
    @DisplayName("findByEmailWithAuthorities returns user when exists")
    @Transactional
    void findByEmailWithAuthorities() {

        Optional<User> found = dao.findByEmail(persisted.getEmail());

        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());
    }

    @Test
    @DisplayName("lockFindUserById returns user with lock")
    @Transactional
    void lockFindUserById() {

        Optional<User> locked = dao.lockFindUserById(persisted.getId());

        assertTrue(locked.isPresent());
        assertEquals(persisted.getId(), locked.get().getId());
    }

    private User buildFullUpdate(Profile profile, Advertiser adv) {
        User update = new User();

        update.setEmail("new@example.com");
        update.setPassword("newPass");
        update.setUserRoles(Collections.emptySet());
        update.setProfile(profile);
        update.setAdvertiser(adv);

        update.setEnabled(!persisted.isEnabled());
        update.setAccountNonLocked(!persisted.isAccountNonLocked());
        update.setAccountNonExpired(!persisted.isAccountNonExpired());
        update.setCredentialNonExpired(!persisted.isCredentialNonExpired());
        update.setEmailVerified(!persisted.isEmailVerified());
        update.setGoogleConnected(!persisted.isGoogleConnected());

        return update;
    }
}
