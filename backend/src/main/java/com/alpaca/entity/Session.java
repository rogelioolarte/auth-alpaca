package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a user session / token-family in the system.
 *
 * <p>A Session (or "family") groups together one or more refresh tokens that belong to the same
 * logical login context (e.g. device + client). This allows tracking and revoking of all tokens in
 * that session, keeping metadata (like IP, user-agent), and marking session as revoked.
 *
 * <p>This entity is mapped to table <code>sessions</code> in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sessions")
@EntityListeners(AuditingEntityListener.class)
public class Session extends Auditable {

    /**
     * Primary key (UUID) of the session/family. This identifies uniquely a login session or
     * token-family.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID id;

    /** The {@link User} this session belongs to. Many sessions may belong to a single user. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Family identifier for the token rotation chain. All refresh tokens issued in the same session
     * will share this UUID.
     */
    @Column(name = "family_id", nullable = false, unique = true)
    private UUID familyId;

    /** Timestamp when this session was created (login / first token issued). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Timestamp when this session was last used (e.g. last refresh or activity). */
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /**
     * IP address from which the session was initiated (or last seen). Useful for audit / security
     * monitoring.
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User-agent string of the client (browser / device) associated with the session. Useful for
     * audit / security monitoring.
     */
    @Column(name = "user_agent")
    private String userAgent;

    /**
     * Identifier for the client or application (if your system supports multiple clients).
     * Optional, depending on your multi-client setup.
     */
    @Column(name = "client_id")
    private String clientId;

    /**
     * Indicates whether this session has been revoked. Once revoked, all associated refresh tokens
     * should be considered invalid.
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /**
     * Timestamp when the session was revoked (if revoked = true). Useful for audit and
     * reuse-detection logic.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Optional reason for revocation (e.g. logout, reuse-detected, admin-revoke). Useful to track
     * why a session was invalidated.
     */
    @Column(name = "revoke_reason")
    private String revokeReason;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Session session)) return false;
        return revoked == session.revoked
                && Objects.equals(id, session.id)
                && Objects.equals(familyId, session.familyId)
                && Objects.equals(ipAddress, session.ipAddress)
                && Objects.equals(userAgent, session.userAgent)
                && Objects.equals(clientId, session.clientId)
                && Objects.equals(revokeReason, session.revokeReason)
                && Objects.equals(user, session.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, user, familyId, ipAddress, userAgent, clientId, revoked, revokeReason);
    }
}
