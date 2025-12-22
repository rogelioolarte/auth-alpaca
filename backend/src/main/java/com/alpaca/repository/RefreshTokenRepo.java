package com.alpaca.repository;

import com.alpaca.entity.RefreshToken;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link RefreshToken} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for refreshToken-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface RefreshTokenRepo extends GenericRepo<RefreshToken, UUID> {}
