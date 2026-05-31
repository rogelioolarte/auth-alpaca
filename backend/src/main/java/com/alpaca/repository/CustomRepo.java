package com.alpaca.repository;

import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Generic repository interface for managing entities.
 *
 * <p>This interface extends {@link JpaRepository} to provide standard CRUD operations and defines
 * additional custom queries for entity operations.
 *
 * <p>The {@link NoRepositoryBean} annotation ensures that Spring does not create an instance of
 * this interface directly.
 *
 * @param <T> The entity type.
 * @param <I> The primary key type of the entity.
 * @see JpaRepository
 */
@NoRepositoryBean
public interface CustomRepo<T, I> extends JpaRepository<T, I> {

    /**
     * Counts the number of entities with the given IDs.
     *
     * @param id The collection of entity IDs to count - must not be null.
     * @throws IllegalStateException if the entity doesn't have an ID
     * @throws EmptyResultDataAccessException if The entity was already deleted
     */
    void deleteById(@NonNull I id);

    /**
     * Counts the number of entities with the given IDs.
     *
     * @param ids The collection of entity IDs to count - must not be null.
     * @return The number of entities found matching the provided IDs.
     */
    long countEntitiesIds(Collection<I> ids);
}
