package com.alpaca.repository;

import com.alpaca.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for user-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface UserRepo extends GenericRepo<User, UUID> {

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address of the user - must not be null.
     * @return An {@link Optional} containing the user if found, otherwise empty.
     */
    @EntityGraph(value = "User.withAuthorities", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the specified email address exists.
     *
     * @param email The email address to check - must not be null.
     * @return {@code true} if a user with the given email exists, {@code false} otherwise.
     */
    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> lockFindUserById(@Param("userId") UUID userId);

    /**
     * Counts the number of entities with the given IDs.
     *
     * @param ids The collection of entity IDs to count - must not be null.
     * @return The number of entities found matching the provided IDs.
     */
    @Query("SELECT COUNT(e) FROM User e WHERE e.id IN :ids")
    long countByIds(@Param("ids") Collection<UUID> ids);
}
