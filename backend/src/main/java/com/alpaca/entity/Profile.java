package com.alpaca.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Profile entity in the system. This entity is mapped to the "profiles" table in the
 * database and stores personal details of a user, including name, address, and avatar. It has a
 * one-to-one relationship with the {@link User} entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profiles")
public class Profile {

    /**
     * Unique identifier for the Profile. This value is automatically generated using a UUID
     * strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id")
    private UUID id;

    /** The first name of the Profile. This field cannot be null. */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /** The last name of the Profile. This field cannot be null. */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** The address of the Profile. This field cannot be null. */
    @Column(name = "address", nullable = false)
    private String address;

    /** URL of the user's avatar image. This field cannot be null. */
    @Column(name = "avatar_url", nullable = false)
    private String avatarUrl;

    /**
     * The User associated with this Profile. This is a one-to-one relationship, meaning each user
     * has exactly one profile.
     */
    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /**
     * Constructs an instance of a new Profile object with the specified attributes. The generated
     * object is ready to be used and stored in the database.
     *
     * @param firstName First name - must not be null
     * @param lastName Last name - must not be null
     * @param address Address - must not be null
     * @param avatarUrl URL of the profile's avatar - must not be null
     * @param user The associated User instance - must not be null
     */
    public Profile(String firstName, String lastName, String address, String avatarUrl, User user) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.user = user;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Profile profile)) return false;
        return firstName != null
                && firstName.equals(profile.firstName)
                && lastName != null
                && lastName.equals(profile.lastName)
                && address != null
                && address.equals(profile.address)
                && avatarUrl != null
                && avatarUrl.equals(profile.avatarUrl)
                && user != null
                && user.equals(profile.user);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(firstName);
        result = 31 * result + Objects.hashCode(lastName);
        result = 31 * result + Objects.hashCode(address);
        result = 31 * result + Objects.hashCode(avatarUrl);
        result = 31 * result + Objects.hashCode(user);
        return result;
    }
}
