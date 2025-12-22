package com.alpaca.repository;

import com.alpaca.entity.Session;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Session} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for session-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface SessionRepo extends GenericRepo<Session, UUID> {}
