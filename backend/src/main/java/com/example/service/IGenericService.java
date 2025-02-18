package com.example.service;

import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Generic service interface providing common CRUD operations.
 *
 * @param <T>  The type of entity.
 * @param <ID> The type of the entity's identifier.
 */
public interface IGenericService<T, ID> {

    /**
     * Finds an entity by its identifier.
     *
     * @param id The identifier of the entity - must not be null.
     * @return The entity if found.
     * @throws BadRequestException if the ID is null.
     * @throws NotFoundException   if the entity is not found.
     */
    T findById(ID id);

    /**
     * Finds all entities by their identifiers.
     *
     * @param ids A collection of entity identifiers - must not be null or empty.
     * @return A list of found entities.
     * @throws BadRequestException if the IDs are null or empty.
     * @throws NotFoundException   if any entity is not found.
     */
    List<T> findAllByIds(Collection<ID> ids);

    /**
     * Finds all entities by their identifiers and returns them as a set.
     *
     * @param ids A collection of entity identifiers - must not be null or empty.
     * @return A set of found entities.
     * @throws BadRequestException if the IDs are null or empty.
     * @throws NotFoundException   if any entity is not found.
     */
    Set<T> findAllByIdsToSet(Collection<ID> ids);

    /**
     * Saves a new entity.
     *
     * @param t The entity to save - must not be null.
     * @return The saved entity.
     * @throws BadRequestException if the entity is null or already exists.
     */
    T save(T t);

    /**
     * Saves multiple entities.
     *
     * @param t A list of entities to save - must not be null or empty.
     * @return A list of saved entities.
     * @throws BadRequestException if the list is null or empty.
     */
    List<T> saveAll(List<T> t);

    /**
     * Updates an existing entity by its identifier.
     *
     * @param t  The updated entity data - must not be null.
     * @param id The identifier of the entity to update - must not be null.
     * @return The updated entity.
     * @throws BadRequestException if the ID or entity is null.
     * @throws NotFoundException   if the entity does not exist.
     */
    T updateById(T t, ID id);

    /**
     * Deletes an entity by its identifier.
     *
     * @param id The identifier of the entity to delete - must not be null.
     * @throws BadRequestException if the ID is null.
     * @throws NotFoundException   if the entity does not exist.
     */
    void deleteById(ID id);

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
     * @throws BadRequestException if the pageable parameter is null.
     */
    Page<T> findAllPage(Pageable pageable);

    /**
     * Checks if an entity exists by its identifier.
     *
     * @param id The identifier of the entity - must not be null.
     * @return {@code true} if the entity exists, otherwise {@code false}.
     */
    boolean existsById(ID id);

    /**
     * Checks if multiple entities exist by their identifiers.
     *
     * @param ids A collection of entity identifiers - must not be null or empty.
     * @return {@code true} if all entities exist, otherwise {@code false}.
     */
    boolean existsAllByIds(Collection<ID> ids);

    /**
     * Checks if an entity exists based on its unique properties.
     *
     * @param t The entity containing unique properties to check - must not be null.
     * @return {@code true} if an entity with the same unique properties exists, otherwise {@code false}.
     */
    boolean existsByUniqueProperties(T t);
}
