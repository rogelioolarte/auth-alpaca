package com.example.repository;

import com.example.entity.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfileRepo extends GenericRepo<Profile, UUID> {

    @Query("SELECT COUNT(p) FROM Profile p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
