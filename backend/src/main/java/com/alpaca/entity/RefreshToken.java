package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents a stored refresh token in the system, used for refresh-token rotation and reuse
 * detection.
 *
 * <p>This entity stores only a cryptographic hash of the token (never the raw token), along with
 * metadata such as which "family" (session / device) it belongs to, expiration, and whether it has
 * been revoked or replaced. This enables secure refresh token rotation and the ability to detect
 * and revoke token misuse or reuse.
 *
 * <p>Mapped to database table <code>refresh_tokens</code>.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken extends Auditable {

    /**
     * Primary key (UUID) of the refresh token record. Identifies uniquely this token entry in the
     * database.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The {@link User} to whom this refresh token belongs. Many refresh tokens may belong to the
     * same user (across sessions / devices).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", updatable = false, nullable = false)
    private User user;

    /**
     * SHA-256 (or similar) hash of the refresh token string. Stored instead of the raw token to
     * avoid token exposure if the database is compromised.
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /**
     * JTI (JWT ID) or unique identifier embedded inside the token, if your token includes one.
     * Useful for traceability or cross-referencing with other token metadata.
     */
    @Column(name = "token_jti")
    private UUID tokenJti;

    /**
     * Identifier for the "family" or session this token belongs to (e.g. device / client). All
     * tokens issued as part of the same login flow / device should share the same familyId. Allows
     * revoking or invalidating entire families at once (e.g. logout all tokens from device).
     */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    /**
     * Reference to another RefreshToken that replaced this one (after a rotation). Once this token
     * is used for rotation, this property links to the new token. Enables reuse-detection: if the
     * old token is used again, you can detect misuse.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by")
    private RefreshToken replacedBy;

    /**
     * Indicates whether this token has been revoked (explicit logout, reuse detection, admin
     * revocation, etc.). Once revoked, the token must not be accepted any longer.
     */
    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /** Timestamp when this token was revoked (if revoked = true). Null if token is still active. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Expiration timestamp — the time after which this token should no longer be valid. Must be
     * checked upon token use.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when this token was last used (e.g. for refresh). Useful for auditing and token-use
     * tracking.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * Identifier of client or application (if your system supports multiple clients), stored for
     * audit or token-management purposes.
     */
    @Column(name = "client_id")
    private String clientId;

    /**
     * IP address from which the token was issued or last used. Useful for logging, auditing, and
     * detecting suspicious activity.
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User-agent string of the client (browser / device) associated with this token. Helps with
     * audit and security analysis.
     */
    @Column(name = "user_agent")
    private String userAgent;

    /**
     * Optional string indicating the reason why this token was revoked or replaced (e.g. "logout",
     * "reuse-detected", "admin-revoked", etc.). Useful for audit trails.
     */
    @Column(name = "revoke_reason")
    private String revokeReason;

    public RefreshToken(
            RefreshToken previous,
            UUID tokenJti,
            Instant expiresAt,
            Instant lastUsedAt,
            String clientId,
            String userAgent,
            String ipAddress) {
        this.user = previous.getUser();
        this.tokenJti = tokenJti;
        this.familyId = previous.getFamilyId();
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.clientId = clientId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public RefreshToken(Session session, UUID tokenJti, Instant expiresAt, Instant lastUsedAt) {
        this.user = session.getUser();
        this.tokenJti = tokenJti;
        this.familyId = session.getFamilyId();
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.clientId = session.getClientId();
        this.ipAddress = session.getIpAddress();
        this.userAgent = session.getUserAgent();
        this.revoked = false;
        this.revokedAt = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return Objects.equals(tokenHash, that.tokenHash)
                && Objects.equals(tokenJti, that.tokenJti)
                && Objects.equals(familyId, that.familyId)
                && Objects.equals(revoked, that.revoked)
                && Objects.equals(clientId, that.clientId)
                && Objects.equals(ipAddress, that.ipAddress)
                && Objects.equals(userAgent, that.userAgent)
                && Objects.equals(revokeReason, that.revokeReason)
                && Objects.equals(user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                user.getId(),
                tokenHash,
                tokenJti,
                familyId,
                revoked,
                clientId,
                ipAddress,
                userAgent,
                revokeReason);
    }
}
