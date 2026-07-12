package com.alpaca.persistence;

import com.alpaca.entity.Profile;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@code Profile} entities. Extends {@link
 * IGenericDAO} to inherit common CRUD operations.
 *
 * @see IGenericDAO
 */
public interface IProfileDAO extends IGenericDAO<Profile, UUID> {}
