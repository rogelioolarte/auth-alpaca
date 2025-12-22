package com.alpaca.persistence;

import com.alpaca.entity.RefreshToken;

import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link RefreshToken} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code RefreshToken} management.
 *
 * @see IGenericDAO
 */
public interface IRefreshTokenDAO extends IGenericDAO<RefreshToken, UUID> {}
