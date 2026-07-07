package com.alpaca.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object (DAO) interface providing common CRUD operations.
 *
 * @param <T> The type of entity.
 * @param <I> The type of the entity's identifier.
 */
public interface IGenericDAO<T, I> {

    /**
     * Finds an entity by its identifier.
     *
     * @param i The identifier of the entity - must not be null.
     * @return An {@code Optional} containing the entity if found, otherwise empty.
     */
    Optional<T> findById(I i);

    /**
     * Finds all entities by their identifiers.
     *
     * @param is A collection of entity identifiers - must not be null.
     * @return A list of entities found.
     */
    List<T> findAllByIds(Collection<I> is);

    /**
     * Saves a new entity.
     *
     * @param t The entity to save - must not be null.
     * @return The saved entity.
     */
    T save(T t);

    /**
     * Saves multiple entities in batch.
     *
     * @param t A collection of entities to save - must not be null.
     * @return A list of saved entities.
     */
    List<T> saveAll(Collection<T> t);

    /**
     * Deletes an entity by its identifier - must not be null.
     *
     * @param i The identifier of the entity to delete.
     */
    void deleteById(I i);

    /**
     * Retrieves all entities.
     *
     * @return A list of all entities.
     */
    List<T> findAll();

    /**
     * Retrieves all entities with pagination support.
     *
     * @param pageable The pagination configuration - must not be null.
     * @return A {@code Page} containing the paginated entities.
     */
    Page<T> findAllPage(Pageable pageable);

    /**
     * Checks if an entity exists by its identifier.
     *
     * @param i The identifier of the entity - must not be null.
     * @return {@code true} if the entity exists, otherwise {@code false}.
     */
    boolean existsById(I i);

    /**
     * Checks if multiple entities exist by their identifiers.
     *
     * @param is A collection of entity identifiers - must not be null.
     * @return {@code true} if all entities exist, otherwise {@code false}.
     */
    boolean existsAllByIds(Collection<I> is);

    /**
     * Checks if an entity exists based on its unique properties.
     *
     * @param t The entity containing unique properties to check - must not be null.
     * @return {@code true} if an entity with the same unique properties exists, otherwise {@code
     *     false}.
     */
    boolean existsByUniqueProperties(T t);
}
