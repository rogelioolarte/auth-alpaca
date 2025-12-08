package com.alpaca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Abstract base class for entities that require auditing information, automatically tracking
 * creation and modification metadata.
 *
 * <p>This class is intended to be inherited by all JPA entity classes within the application that
 * need to record:
 *
 * <ul>
 *   <li>When the record was created ({@code createdAt})
 *   <li>When the record was last modified ({@code updatedAt})
 *   <li>Who created the record ({@code createdBy})
 *   <li>Who last modified the record ({@code updatedBy})
 * </ul>
 *
 * <p>It uses Spring Data JPA's {@link AuditingEntityListener} which requires the application to be
 * configured with {@code @EnableJpaAuditing} and an implementation of {@code AuditorAware<String>}
 * for tracking user details.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /**
     * The date and time when the entity was first persisted. This field is managed automatically by
     * Spring Data JPA Auditing. It is set on creation and cannot be updated afterward.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * The date and time when the entity was last modified. This field is updated automatically on
     * every modification of the entity.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * The identifier (e.g., username or ID) of the user who created the entity. This field is set
     * on creation and cannot be updated afterward.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * The identifier (e.g., username or ID) of the user who last modified the entity. This field is
     * updated automatically on every modification of the entity.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // NOTE: It is best practice to include Getters for these fields,
    // but Setters are usually omitted to enforce automatic auditing control.
    // However, I will not add them here as they were not in the original class.
}
