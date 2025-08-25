package com.alpaca.persistence.impl;

import com.alpaca.persistence.IGenericDAO;
import com.alpaca.repository.GenericRepo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Abstract base implementation of {@link IGenericDAO}, providing generic CRUD and pagination
 * operations for any entity type {@code T} with identifier type {@code ID}.
 *
 * <p>Concrete DAO implementations must supply the specific {@link GenericRepo} and entity class by
 * implementing the abstract {@link #getRepo()} and {@link #getEntity()} methods.
 *
 * <p>This class leverages Spring Data repositories to delegate standard persistence operations in a
 * type-safe, reusable fashion.
 *
 * @param <T> entity type managed by this DAO
 * @param <ID> type of the entity's identifier
 */
public abstract class GenericDAOImpl<T, ID> implements IGenericDAO<T, ID> {

    /**
     * Supplies the Spring Data repository for the specific entity type.
     *
     * @return the repository instance for {@code T}
     */
    protected abstract GenericRepo<T, ID> getRepo();

    /**
     * Provides the entity class handled by this DAO implementation.
     *
     * @return the {@code Class} object representing {@code T}
     */
    protected abstract Class<T> getEntity();

    /**
     * Finds an entity by its identifier.
     *
     * @param id the identifier of the entity to find; may be {@code null}
     * @return an {@link Optional} containing the entity if found, otherwise empty
     */
    @Override
    public Optional<T> findById(ID id) {
        return getRepo().findById(id);
    }

    /**
     * Retrieves all entities matching the provided collection of identifiers.
     *
     * @param ids the collection of identifiers; may be {@code null} or empty
     * @return a {@link List} of entities found; empty if none match
     */
    @Override
    public List<T> findAllByIds(Collection<ID> ids) {
        return getRepo().findAllById(ids);
    }

    /**
     * Deletes the entity identified by the given ID, if it exists.
     *
     * @param id the identifier of the entity to delete; may be {@code null}
     */
    @Override
    public void deleteById(ID id) {
        getRepo().deleteById(id);
    }

    /**
     * Saves or updates the given entity.
     *
     * @param t the entity to save; must not be {@code null}
     * @return the saved entity instance
     */
    @Override
    public T save(T t) {
        return getRepo().save(t);
    }

    /**
     * Saves or updates a collection of entities.
     *
     * @param t the collection of entities to save; must not be {@code null}
     * @return a {@link List} of saved entity instances
     */
    @Override
    public List<T> saveAll(Collection<T> t) {
        return getRepo().saveAll(t);
    }

    /**
     * Retrieves all entities of type {@code T}.
     *
     * @return a {@link List} of all entities; empty if none exist
     */
    @Override
    public List<T> findAll() {
        return getRepo().findAll();
    }

    /**
     * Retrieves entities in a paginated fashion using the provided {@link Pageable}.
     *
     * @param pageable pagination and sorting parameters; must not be {@code null}
     * @return a {@link Page} of entities
     */
    @Override
    public Page<T> findAllPage(Pageable pageable) {
        return getRepo().findAll(pageable);
    }

    /**
     * Checks whether an entity exists with the given identifier.
     *
     * @param id the identifier to check; may be {@code null}
     * @return {@code true} if an entity exists with the specified ID; {@code false} otherwise
     */
    @Override
    public boolean existsById(ID id) {
        return getRepo().existsById(id);
    }

    /**
     * Verifies whether all entities corresponding to the provided identifiers exist.
     *
     * @param ids the collection of IDs to check; may be {@code null}
     * @return {@code true} if the count of matching entities equals the number of IDs provided;
     *     {@code false} otherwise
     */
    @Override
    public boolean existsAllByIds(Collection<ID> ids) {
        return ((long) ids.size()) == getRepo().countByIds(ids);
    }
}
