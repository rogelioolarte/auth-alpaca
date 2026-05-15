package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an Advertiser entity in the system. This entity is mapped to the "advertisers" table
 * in the database and stores details about businesses or individuals promoting content. It includes
 * information about their title, description, location, and verification status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "advertisers")
public class Advertiser extends Auditable {

    /**
     * Unique identifier for the Advertiser. This value is automatically generated using a UUID
     * strategy.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id")
    private UUID id;

    /** The title or name of the Advertiser. This field cannot be null. */
    @Column(name = "title", nullable = false)
    private String title;

    /** A brief description of the Advertiser. This field cannot be null. */
    @Column(name = "description", nullable = false)
    private String description;

    /** URL of the banner image associated with the Advertiser. This field cannot be null. */
    @Column(name = "banner_url", nullable = false)
    private String bannerUrl;

    /** URL of the Advertiser's avatar image. This field cannot be null. */
    @Column(name = "avatar_url", nullable = false)
    private String avatarUrl;

    /** Publicly visible location of the Advertiser. This field cannot be null. */
    @Column(name = "public_location", nullable = false)
    private String publicLocation;

    /** URL link to the Advertiser's public location. This field cannot be null. */
    @Column(name = "public_url_location", nullable = false)
    private String publicUrlLocation;

    /** Indicates whether the Advertiser is indexed for public search. */
    @Column(name = "indexed", nullable = false)
    private boolean indexed = true;

    /** Indicates whether the Advertiser has a paid subscription or promotion. */
    @Column(name = "paid", nullable = false)
    private boolean paid = false;

    /** Indicates whether the Advertiser is verified. */
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    /**
     * The User associated with the Advertiser entity. A one-to-one relationship exists between User
     * and Advertiser.
     */
    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Advertiser that)) return false;
        return indexed == that.indexed
                && paid == that.paid
                && verified == that.verified
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(bannerUrl, that.bannerUrl)
                && Objects.equals(avatarUrl, that.avatarUrl)
                && Objects.equals(publicLocation, that.publicLocation)
                && Objects.equals(publicUrlLocation, that.publicUrlLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                title,
                description,
                bannerUrl,
                avatarUrl,
                publicLocation,
                publicUrlLocation,
                indexed,
                paid,
                verified);
    }
}
