package com.example.service;

import com.example.entity.Profile;

import java.util.UUID;

/**
 * Service interface for managing {@code Profile} entities.
 * Extends {@link IGenericService} to inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IProfileService extends IGenericService<Profile, UUID> {
}
