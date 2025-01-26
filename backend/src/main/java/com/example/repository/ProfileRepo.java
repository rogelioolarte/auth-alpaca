package com.example.repository;

import com.example.entity.Profile;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfileRepo extends GenericRepo<Profile, UUID> {
}
