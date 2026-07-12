package com.alpaca.repository;

import com.alpaca.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>Extends {@link CustomRepo} to inherit common CRUD operations and defines additional queries
 * for user-specific operations.
 *
 * @see CustomRepo
 */
@Repository
public interface UserRepo extends CustomRepo<User, UUID> {

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address of the user - must not be null.
     * @return An {@link Optional} containing the user if found, otherwise empty.
     */
    @EntityGraph(value = "User.withAuthorities", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findByEmail(String email);

    /**
     * Retrieves a user by ID, eagerly loading associated roles and permissions.
     *
     * <p>The {@link EntityGraph} ensures that the {@code User.withAuthorities} named entity graph
     * is fetched in a single query, avoiding N+1 selects when the user's authorities are accessed
     * after retrieval.
     *
     * @param id The user UUID - must not be null.
     * @return An {@link Optional} containing the user if found, otherwise empty.
     */
    @NonNull
    @EntityGraph(value = "User.withAuthorities", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findById(@NonNull UUID id);

    /**
     * Checks whether a user with the specified email address exists.
     *
     * @param email The email address to check - must not be null.
     * @return {@code true} if a user with the given email exists, {@code false} otherwise.
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves and pessimistically locks a user row for the duration of the current transaction.
     *
     * <p>This is used in concurrency-sensitive operations (e.g. token rotation, credential updates)
     * where a {@code PESSIMISTIC_WRITE} lock prevents overlapping transactions from reading or
     * modifying the same user record until the lock is released.
     *
     * @param userId The user UUID to lock and retrieve.
     * @return An {@link Optional} containing the locked user if found, otherwise empty.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> lockFindUserById(@Param("userId") UUID userId);
}
