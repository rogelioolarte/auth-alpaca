package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.persistence.impl.ProfileDAOImpl;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link ProfileDAOImpl} */
@DataJpaTest
@Import({ProfileDAOImpl.class})
class ProfileDAOImplIT {

    @Autowired private IProfileDAO dao;

    @Autowired private ProfileRepo repo;

    @Autowired private UserRepo userRepo;

    private Profile saved;
    private User owner;

    @BeforeEach
    void setup() {
        User unsaved = UserProvider.singleTemplate();
        Instant now = Instant.now();
        unsaved.setCreatedAt(now);
        unsaved.setCreatedBy(UUID.randomUUID().toString());
        owner = userRepo.save(unsaved);

        Profile template = ProfileProvider.singleTemplate();
        template.setUser(owner);
        template.setCreatedBy(UUID.randomUUID().toString());
        template.setCreatedAt(now);

        saved = repo.save(template);
    }

    @Test
    @DisplayName("findById returns empty when missing and present when exists")
    @Transactional
    void findById() {

        UUID missingId = UUID.randomUUID();
        assertTrue(dao.findById(missingId).isEmpty());

        Profile found = dao.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
    }

    @Test
    @DisplayName("updateById updates non-null/blank fields only")
    @Transactional
    void updateById() {

        UUID id = saved.getId();

        Profile update = new Profile();
        update.setFirstName("Jane");
        update.setLastName("Smith");
        update.setAddress("456 Elm St");
        update.setAvatarUrl("http://avatar/jane");

        User newUser = userRepo.save(UserProvider.alternativeTemplate());
        update.setUser(newUser);

        Profile out = dao.updateById(update, id);

        assertAll(
                () -> assertEquals(id, out.getId()),
                () -> assertEquals("Jane", out.getFirstName()),
                () -> assertEquals("Smith", out.getLastName()),
                () -> assertEquals("456 Elm St", out.getAddress()),
                () -> assertEquals("http://avatar/jane", out.getAvatarUrl()),
                () -> assertEquals(newUser.getId(), out.getUser().getId()));
    }

    @Test
    @DisplayName("updateById ignores null and blank values")
    @Transactional
    void updateByIdPartial() {

        UUID id = saved.getId();

        Profile partial = new Profile();
        partial.setFirstName("Alice");
        partial.setLastName("  ");
        partial.setAvatarUrl("");

        Profile out = dao.updateById(partial, id);

        assertEquals("Alice", out.getFirstName());
        assertEquals(saved.getLastName(), out.getLastName());
        assertEquals(saved.getAddress(), out.getAddress());
        assertEquals(saved.getAvatarUrl(), out.getAvatarUrl());
        assertEquals(saved.getUser().getId(), out.getUser().getId());
    }

    @Test
    @DisplayName("updateById does not update user when same id")
    @Transactional
    void updateByIdSameUser() {

        Profile update = new Profile();
        update.setUser(owner);

        Profile out = dao.updateById(update, saved.getId());

        assertEquals(owner.getId(), out.getUser().getId());
    }

    @Test
    @DisplayName("updateById updates user when existing profile has null user")
    @Transactional
    void updateByIdExistingUserNull() {

        Profile profileWithoutUser = ProfileProvider.singleTemplate();
        profileWithoutUser.setUser(null);

        Profile stored = repo.save(profileWithoutUser);

        User newUser = userRepo.save(UserProvider.alternativeTemplate());

        Profile update = new Profile();
        update.setUser(newUser);

        Profile out = dao.updateById(update, stored.getId());

        assertEquals(newUser.getId(), out.getUser().getId());
    }

    @Test
    @DisplayName("updateById throws NotFoundException when id does not exist")
    @Transactional
    void updateByIdNotFound() {

        Profile update = new Profile();
        update.setFirstName("Ghost");

        assertThrows(NotFoundException.class, () -> dao.updateById(update, UUID.randomUUID()));
    }

    @Test
    @DisplayName("existsByUniqueProperties covers null, missing id and existing cases")
    @Transactional
    void existsByUniqueProperties() {

        Profile noUser = ProfileProvider.singleTemplate();
        noUser.setUser(null);
        noUser.setCreatedAt(Instant.now());
        noUser.setCreatedBy(UUID.randomUUID().toString());

        assertFalse(dao.existsByUniqueProperties(noUser));

        Profile noId = ProfileProvider.singleTemplate();
        noId.setUser(new User());
        noId.setCreatedAt(Instant.now());
        noId.setCreatedBy(UUID.randomUUID().toString());

        assertFalse(dao.existsByUniqueProperties(noId));

        Profile check = ProfileProvider.singleTemplate();
        check.setUser(owner);

        assertTrue(dao.existsByUniqueProperties(check));
    }
}
