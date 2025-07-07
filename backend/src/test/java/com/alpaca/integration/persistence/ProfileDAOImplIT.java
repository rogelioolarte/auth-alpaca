package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link com.alpaca.persistence.impl.ProfileDAOImpl}
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class ProfileDAOImplIT {

    @Autowired
    private IProfileDAO dao;

    @Autowired
    private ProfileRepo repo;

    @Autowired
    private UserRepo userRepo;

    private Profile saved;
    private User owner;

    @BeforeEach
    void setup() {
        // create and persist a User for association
        owner = userRepo.save(UserProvider.singleTemplate());
        // prepare a profile for tests
        Profile template = ProfileProvider.singleTemplate();
        template.setUser(owner);
        saved = repo.save(template);
    }

    @Test
    @DisplayName("findById returns empty when missing and present when exists")
    @Transactional
    void findById() {
        UUID missingId = UUID.randomUUID();
        assertTrue(dao.findById(missingId).isEmpty());

        assertTrue(dao.findById(saved.getId()).isPresent());
        assertEquals(saved.getId(), dao.findById(saved.getId()).get().getId());
    }

    @Test
    @DisplayName("updateById updates non-null/blank fields only and throws if not found")
    @Transactional
    void updateById() {
        UUID id = saved.getId();
        // full update
        Profile update = new Profile();
        update.setFirstName("Jane");
        update.setLastName("Smith");
        update.setAddress("456 Elm St");
        update.setAvatarUrl("http://avatar/jane");
        User newUser = userRepo.save(UserProvider.alternativeTemplate());
        update.setUser(newUser);

        Profile out = dao.updateById(update, id);
        assertEquals(id, out.getId());
        assertEquals("Jane", out.getFirstName());
        assertEquals("Smith", out.getLastName());
        assertEquals("456 Elm St", out.getAddress());
        assertEquals("http://avatar/jane", out.getAvatarUrl());
        assertEquals(newUser.getId(), out.getUser().getId());

        // partial update: null or blank ignored
        Profile partial = new Profile();
        partial.setFirstName("Alice");
        // lastName blank, address null, avatarUrl blank, user null
        partial.setLastName("  ");
        partial.setAvatarUrl("");
        Profile outPartial = dao.updateById(partial, id);
        assertEquals("Alice", outPartial.getFirstName());
        // unchanged fields
        assertEquals(out.getLastName(), outPartial.getLastName());
        assertEquals(out.getAddress(), outPartial.getAddress());
        assertEquals(out.getAvatarUrl(), outPartial.getAvatarUrl());
        assertEquals(out.getUser().getId(), outPartial.getUser().getId());

        // non-existing id
        assertThrows(NotFoundException.class,
            () -> dao.updateById(update, UUID.randomUUID()));
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false for null user or missing id and true when exists")
    @Transactional
    void existsByUniqueProperties() {
        // null user
        Profile noUser = ProfileProvider.singleEntity();
        noUser.setUser(null);
        assertFalse(dao.existsByUniqueProperties(noUser));

        // user without id
        Profile noId = ProfileProvider.singleEntity();
        noId.setUser(new User());
        assertFalse(dao.existsByUniqueProperties(noId));

        // saved case
        Profile check = ProfileProvider.singleEntity();
        check.setUser(owner);
        assertTrue(dao.existsByUniqueProperties(check));
    }
}
