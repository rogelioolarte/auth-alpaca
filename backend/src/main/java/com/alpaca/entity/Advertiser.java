package com.alpaca.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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
public class Advertiser {

    /**
     * Unique identifier for the Advertiser. This value is automatically generated using a UUID
     * strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "advertiser_id")
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

    /**
     * Constructs an instance of a new Advertiser object with the specified attributes. The
     * generated object is ready to be used and stored in the database.
     *
     * @param title Title name - must not be null
     * @param description Short description of the Advertiser - must not be null
     * @param bannerUrl URL of the Advertiser's banner image - must not be null
     * @param avatarUrl URL of the Advertiser's avatar image - must not be null
     * @param publicLocation Publicly visible location - must not be null
     * @param publicUrlLocation URL of the public location - must not be null
     * @param indexed indicates whether the Advertiser is indexed for public search
     * @param paid indicates whether the Advertiser has a paid subscription
     * @param verified indicates whether the Advertiser is verified
     * @param user the User associated with this Advertiser - must not be null
     */
    public Advertiser(
            String title,
            String description,
            String bannerUrl,
            String avatarUrl,
            String publicLocation,
            String publicUrlLocation,
            boolean indexed,
            boolean paid,
            boolean verified,
            User user) {
        this.title = title;
        this.description = description;
        this.bannerUrl = bannerUrl;
        this.avatarUrl = avatarUrl;
        this.publicLocation = publicLocation;
        this.publicUrlLocation = publicUrlLocation;
        this.indexed = indexed;
        this.paid = paid;
        this.verified = verified;
        this.user = user;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Advertiser that)) return false;
        return indexed == that.indexed
                && paid == that.paid
                && verified == that.verified
                && title != null
                && title.equals(that.title)
                && description != null
                && description.equals(that.description)
                && bannerUrl != null
                && bannerUrl.equals(that.bannerUrl)
                && avatarUrl != null
                && avatarUrl.equals(that.avatarUrl)
                && publicLocation != null
                && publicLocation.equals(that.publicLocation)
                && publicUrlLocation != null
                && publicUrlLocation.equals(that.publicUrlLocation)
                && user != null
                && user.equals(that.user);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + bannerUrl.hashCode();
        result = 31 * result + avatarUrl.hashCode();
        result = 31 * result + publicLocation.hashCode();
        result = 31 * result + publicUrlLocation.hashCode();
        result = 31 * result + Boolean.hashCode(indexed);
        result = 31 * result + Boolean.hashCode(paid);
        result = 31 * result + Boolean.hashCode(verified);
        result = 31 * result + user.hashCode();
        return result;
    }
}
