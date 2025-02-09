package com.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "advertisers")
public class Advertiser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "advertiser_id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "banner_url", nullable = false)
    private String bannerUrl;

    @Column(name = "avatar_url", nullable = false)
    private String avatarUrl;

    @Column(name = "public_location", nullable = false)
    private String publicLocation;

    @Column(name = "public_url_location", nullable = false)
    private String publicUrlLocation;

    @Column(name = "indexed", nullable = false)
    private boolean indexed;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    public Advertiser(String title, String description, String bannerUrl,
                      String avatarUrl, String publicLocation, String publicUrlLocation,
                      boolean indexed, boolean paid, boolean verified, User user) {
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
}
