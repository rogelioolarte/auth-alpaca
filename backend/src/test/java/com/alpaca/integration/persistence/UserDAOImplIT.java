package com.alpaca.integration.persistence;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link com.alpaca.persistence.impl.UserDAOImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class UserDAOImplIT {

    @Autowired private IUserDAO dao;

    @Autowired private UserRepo userRepo;

    @Autowired private ProfileRepo profileRepo;

    @Autowired private AdvertiserRepo advertiserRepo;

    private User template;
    private User persisted;

    @BeforeEach
    void setUp() {
        // prepare a user for update and findByEmail
        template = UserProvider.singleTemplate();
        persisted = userRepo.save(template);
    }

    @Test
    @DisplayName("findByEmail returns empty for null or blank and finds existing user by email")
    @Transactional
    void findByEmail() {
        // null email
        assertTrue(dao.findByEmail(null).isEmpty());
        // blank email
        assertTrue(dao.findByEmail("  ").isEmpty());
        // existing
        Optional<User> found = dao.findByEmail(persisted.getEmail());
        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());
    }

    @Test
    @DisplayName("updateById updates non-null fields, toggles booleans, and throws if not found")
    @Transactional
    void updateById() {
        UUID id = persisted.getId();
        // prepare related entities
        Profile templateProfile = ProfileProvider.singleTemplate();
        Advertiser templateAdvertiser = AdvertiserProvider.singleTemplate();
        templateProfile.setUser(persisted);
        templateAdvertiser.setUser(persisted);
        Profile profile = profileRepo.save(templateProfile);
        Advertiser adv = advertiserRepo.save(templateAdvertiser);

        // Full update request
        User update = new User();
        update.setEmail("new@example.com");
        update.setPassword("newPass");
        update.setUserRoles(Collections.emptySet());
        update.setProfile(profile);
        update.setAdvertiser(adv);
        update.setEnabled(!persisted.isEnabled());
        update.setAccountNoLocked(!persisted.isAccountNoLocked());
        update.setAccountNoExpired(!persisted.isAccountNoExpired());
        update.setCredentialNoExpired(!persisted.isCredentialNoExpired());
        update.setEmailVerified(!persisted.isEmailVerified());
        update.setGoogleConnected(!persisted.isGoogleConnected());

        User out = dao.updateById(update, id);
        assertEquals(id, out.getId());
        assertEquals("new@example.com", out.getEmail());
        assertEquals("newPass", out.getPassword());
        assertEquals(profile.getId(), out.getProfile().getId());
        assertEquals(adv.getId(), out.getAdvertiser().getId());
        assertEquals(update.isEnabled(), out.isEnabled());
        assertEquals(update.isAccountNoLocked(), out.isAccountNoLocked());
        assertEquals(update.isAccountNoExpired(), out.isAccountNoExpired());
        assertEquals(update.isCredentialNoExpired(), out.isCredentialNoExpired());
        assertEquals(update.isEmailVerified(), out.isEmailVerified());
        assertEquals(update.isGoogleConnected(), out.isGoogleConnected());

        // partial update: null or blank should not override
        User partial = new User();
        partial.setEmail("partial@example.com");
        // leave password blank
        User outPartial = dao.updateById(partial, id);
        assertEquals("partial@example.com", outPartial.getEmail());
        assertEquals(out.getPassword(), outPartial.getPassword());

        // non-existing id
        assertThrows(NotFoundException.class, () -> dao.updateById(update, UUID.randomUUID()));
    }

    @Test
    @DisplayName("existsByEmail and existsByUniqueProperties behave correctly")
    @Transactional
    void existsByEmailAndUnique() {
        // null or blank
        assertFalse(dao.existsByEmail(null));
        assertFalse(dao.existsByEmail("  "));
        // existing
        assertTrue(dao.existsByEmail(persisted.getEmail()));
        // uniqueProperties same as email
        assertTrue(dao.existsByUniqueProperties(persisted));
    }
}
