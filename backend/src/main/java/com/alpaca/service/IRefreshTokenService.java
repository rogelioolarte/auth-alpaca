package com.alpaca.service;

import com.alpaca.entity.RefreshToken;
import java.util.UUID;

/**
 * Service interface for managing {@link RefreshToken} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IRefreshTokenService extends IGenericService<RefreshToken, UUID> {}
