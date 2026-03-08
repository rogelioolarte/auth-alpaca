package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.UserRepo;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AdvertiserDAOImpl} */
@DataJpaTest
@Import({AdvertiserDAOImpl.class})
class AdvertiserDAOImplIT {

    @Autowired private IAdvertiserDAO dao;

    @Autowired private AdvertiserRepo repo;

    @Autowired private UserRepo userRepo;

    private User user;
    private User templateUser;

    @BeforeEach
    void setup() {
        templateUser = new User("template@mail.com", "password", Collections.emptySet());
        templateUser.setCreatedAt(Instant.now());
        templateUser.setCreatedBy(UUID.randomUUID().toString());
        user = userRepo.save(templateUser);
    }

    private Advertiser createNew(String title) {
        Advertiser adv = new Advertiser();
        adv.setTitle(title);
        adv.setDescription("Description for " + title);
        adv.setAvatarUrl("https://avatar/" + title);
        adv.setBannerUrl("https://banner/" + title);
        adv.setPublicLocation("Location " + title);
        adv.setPublicUrlLocation("https://loc/" + title);
        adv.setIndexed(true);
        return adv;
    }

    @Test
    @DisplayName("updateById updates fields and ignores blank values")
    @Transactional
    void updateById() {

        Advertiser adv = createNew("Orig");
        adv.setUser(user);

        Advertiser original = repo.save(adv);
        UUID id = original.getId();

        Advertiser update = new Advertiser();
        update.setTitle("NewTitle");
        update.setDescription("NewDesc");
        update.setAvatarUrl("https://new/avatar");
        update.setBannerUrl("https://new/banner");
        update.setPublicLocation("NewLoc");
        update.setPublicUrlLocation("https://new/loc");
        update.setIndexed(!original.isIndexed());

        Advertiser out = dao.updateById(update, id);

        assertAll(
                () -> assertEquals(id, out.getId()),
                () -> assertEquals("NewTitle", out.getTitle()),
                () -> assertEquals("NewDesc", out.getDescription()),
                () -> assertEquals("https://new/avatar", out.getAvatarUrl()),
                () -> assertEquals("https://new/banner", out.getBannerUrl()),
                () -> assertEquals("NewLoc", out.getPublicLocation()),
                () -> assertEquals("https://new/loc", out.getPublicUrlLocation()),
                () -> assertEquals(update.isIndexed(), out.isIndexed()));

        Advertiser partial = new Advertiser();
        partial.setTitle("PartialTitle");

        Advertiser outPartial = dao.updateById(partial, id);

        assertEquals("PartialTitle", outPartial.getTitle());
        assertEquals(out.getDescription(), outPartial.getDescription());
    }

    @Test
    @DisplayName("updateById does not change user when same user id is provided")
    @Transactional
    void updateByIdSameUser() {

        Advertiser adv = createNew("SameUser");
        adv.setUser(user);

        Advertiser stored = repo.save(adv);

        Advertiser update = new Advertiser();
        update.setUser(user);

        Advertiser out = dao.updateById(update, stored.getId());

        assertEquals(user.getId(), out.getUser().getId());
    }

    @Test
    @DisplayName("updateById assigns user when existing advertiser has null user")
    @Transactional
    void updateByIdExistingUserNull() {

        Advertiser adv = createNew("NoUser");
        adv.setUser(null);

        Advertiser stored = repo.save(adv);

        User newUser = userRepo.save(new User("new@mail.com", "password", Collections.emptySet()));

        Advertiser update = new Advertiser();
        update.setUser(newUser);

        Advertiser out = dao.updateById(update, stored.getId());

        assertEquals(newUser.getId(), out.getUser().getId());
    }

    @Test
    @DisplayName("updateById throws NotFoundException when advertiser does not exist")
    @Transactional
    void updateByIdNotFound() {

        Advertiser update = createNew("Ghost");

        assertThrows(NotFoundException.class, () -> dao.updateById(update, UUID.randomUUID()));
    }

    @Test
    @DisplayName("existsByUniqueProperties handles null, missing id, and existing advertiser")
    @Transactional
    void existsByUniqueProperties() {

        Advertiser noUser = createNew("NoUser");
        noUser.setUser(null);

        assertFalse(dao.existsByUniqueProperties(noUser));

        Advertiser withUserNoId = createNew("WU");
        withUserNoId.setUser(templateUser);

        assertFalse(dao.existsByUniqueProperties(withUserNoId));

        Advertiser check = createNew("Check");
        check.setUser(user);

        assertFalse(dao.existsByUniqueProperties(check));

        Advertiser adv = createNew("Saved");
        adv.setUser(user);
        adv.setCreatedAt(Instant.now());
        adv.setCreatedBy(user.getId().toString());
        repo.save(adv);

        Advertiser existsCheck = createNew("Saved");
        existsCheck.setUser(user);

        assertTrue(dao.existsByUniqueProperties(existsCheck));
    }
}
