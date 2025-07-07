package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.UserRepo;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link com.alpaca.persistence.impl.AdvertiserDAOImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class AdvertiserDAOImplIT {

    @Autowired private IAdvertiserDAO dao;

    @Autowired private AdvertiserRepo repo;

    @Autowired private UserRepo userRepo;

    private User singleUser;
    private User templateUser;

    @BeforeEach
    void setup() {
        String title = "Orig";
        templateUser = new User(title + "@mail.com", title + "-password", Collections.emptySet());
        singleUser = userRepo.save(templateUser);
    }

    private Advertiser createNew(String title) {
        Advertiser adv = new Advertiser();
        adv.setTitle(title);
        adv.setDescription("Description for " + title);
        adv.setAvatarUrl("http://avatar/" + title);
        adv.setBannerUrl("http://banner/" + title);
        adv.setPublicLocation("Location " + title);
        adv.setPublicUrlLocation("http://loc/" + title);
        adv.setIndexed(true);
        return adv;
    }

    @Test
    @DisplayName("updateById changes only non-null/blank fields and throws if not found")
    @Transactional
    void updateById() {
        Advertiser adv = createNew("Orig");
        adv.setUser(singleUser);
        Advertiser original = repo.save(adv);
        UUID id = original.getId();

        // full update
        Advertiser update = new Advertiser();
        update.setTitle("NewTitle");
        update.setDescription("NewDesc");
        update.setAvatarUrl("http://new/avatar");
        update.setBannerUrl("http://new/banner");
        update.setPublicLocation("NewLoc");
        update.setPublicUrlLocation("http://new/loc");
        update.setIndexed(!original.isIndexed());
        Advertiser out = dao.updateById(update, id);
        assertEquals(id, out.getId());
        assertEquals("NewTitle", out.getTitle());
        assertEquals("NewDesc", out.getDescription());
        assertEquals("http://new/avatar", out.getAvatarUrl());
        assertEquals("http://new/banner", out.getBannerUrl());
        assertEquals("NewLoc", out.getPublicLocation());
        assertEquals("http://new/loc", out.getPublicUrlLocation());
        assertEquals(update.isIndexed(), out.isIndexed());

        // partial update: blank fields should be ignored
        Advertiser partial = new Advertiser();
        partial.setTitle("PartialTitle");
        // description left null
        Advertiser outPartial = dao.updateById(partial, id);
        assertEquals("PartialTitle", outPartial.getTitle());
        assertEquals(out.getDescription(), outPartial.getDescription());

        // non-existing
        assertThrows(NotFoundException.class, () -> dao.updateById(update, UUID.randomUUID()));
    }

    @Test
    @DisplayName(
            "existsByUniqueProperties returns false for null or no user and true when user exists")
    @Transactional
    void existsByUniqueProperties() {
        // null user
        Advertiser noUser = createNew("NoUser");
        noUser.setUser(null);
        assertFalse(dao.existsByUniqueProperties(noUser));

        // user without ID
        Advertiser withUserNoId = createNew("WU");
        withUserNoId.setUser(templateUser);
        assertFalse(dao.existsByUniqueProperties(withUserNoId));

        // user with ID but no advertiser
        Advertiser check = createNew("WU2");
        check.setUser(singleUser);
        assertFalse(dao.existsByUniqueProperties(check));

        // save advertiser for that user
        Advertiser adv = createNew("WU2");
        adv.setUser(singleUser);
        repo.save(adv);
        Advertiser existsCheck = createNew("WU2");
        existsCheck.setUser(singleUser);
        assertTrue(dao.existsByUniqueProperties(existsCheck));
    }
}
