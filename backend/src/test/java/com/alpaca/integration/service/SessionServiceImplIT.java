package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.SessionServiceImpl;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link SessionServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("SessionServiceImpl Integration Tests")
class SessionServiceImplIT {

    @Autowired private SessionServiceImpl sessionService;
    @Autowired private IUserDAO userDAO;
    @Autowired private ISessionDAO sessionDAO;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private User buildUser() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private User buildAlternativeUser() {
        User user = UserProvider.alternativeTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private Session buildSession() {
        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        return session;
    }

    private Session buildAlternativeSession() {
        Session session = SessionProvider.alternativeTemplate();
        session.setCreatedAt(now);
        return session;
    }

    // -------------------------------------------------------------------------
    // updateById
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateById: should throw when session is null")
    void updateById_ShouldThrow_WhenSessionIsNull() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> sessionService.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session with ID " + id + " cannot be updated");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should throw when id is null")
    void updateById_ShouldThrow_WhenIdIsNull() {

        Session update = buildAlternativeSession();

        assertThatThrownBy(() -> sessionService.updateById(update, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session with ID null cannot be updated");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should throw when session does not exist")
    void updateById_ShouldThrow_WhenSessionDoesNotExist() {

        Session update = buildAlternativeSession();

        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> sessionService.updateById(update, randomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Session with ID " + randomId + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should update all mutable fields")
    void updateById_ShouldUpdateAllMutableFields() {

        User user = buildUser();
        userDAO.save(user);

        User alternativeUser = buildAlternativeUser();
        userDAO.save(alternativeUser);

        Session existing = buildSession();
        existing.setUser(user);
        existing.setRevoked(false);
        existing.setRevokedAt(null);
        existing.setRevokeReason(null);

        Session saved = sessionDAO.save(existing);

        UUID newFamilyId = UUID.randomUUID();

        Instant revokedAt = now.plusSeconds(300);

        Session update = buildAlternativeSession();
        update.setUser(alternativeUser);
        update.setFamilyId(newFamilyId);
        update.setIpAddress("10.10.10.10");
        update.setUserAgent("Updated-Agent");
        update.setClientId("updated-client");
        update.setRevoked(true);
        update.setRevokedAt(revokedAt);
        update.setRevokeReason("manual-revoke");

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(alternativeUser.getId());
        assertThat(result.getFamilyId()).isEqualTo(newFamilyId);
        assertThat(result.getIpAddress()).isEqualTo("10.10.10.10");
        assertThat(result.getUserAgent()).isEqualTo("Updated-Agent");
        assertThat(result.getClientId()).isEqualTo("updated-client");
        assertThat(result.isRevoked()).isTrue();
        assertThat(result.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(result.getRevokeReason()).isEqualTo("manual-revoke");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore blank text values")
    void updateById_ShouldIgnoreBlankTextValues() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        String originalIp = saved.getIpAddress();
        String originalAgent = saved.getUserAgent();
        String originalClient = saved.getClientId();

        Session update = buildAlternativeSession();
        update.setIpAddress(" ");
        update.setUserAgent(" ");
        update.setClientId(" ");

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getIpAddress()).isEqualTo(originalIp);
        assertThat(result.getUserAgent()).isEqualTo(originalAgent);
        assertThat(result.getClientId()).isEqualTo(originalClient);
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore null familyId and revokedAt")
    void updateById_ShouldIgnoreNullFields() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        UUID originalFamilyId = existing.getFamilyId();

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();
        update.setFamilyId(null);
        update.setRevokedAt(null);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getFamilyId()).isEqualTo(originalFamilyId);
        assertThat(result.getRevokedAt()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("updateById: should not replace user when ids are equal")
    void updateById_ShouldNotReplaceUser_WhenIdsAreEqual() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();

        User sameUserReference = new User();
        sameUserReference.setId(user.getId());

        update.setUser(sameUserReference);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore user when incoming user id is null")
    void updateById_ShouldIgnoreUser_WhenIncomingUserIdIsNull() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();

        User incomingUser = new User();
        incomingUser.setId(null);

        update.setUser(incomingUser);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore null user")
    void updateById_ShouldIgnoreNullUser() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();
        update.setUser(null);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(user.getId());
    }
}
