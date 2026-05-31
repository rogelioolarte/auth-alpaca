package com.alpaca.persistence.impl;

import com.alpaca.persistence.IGenericDAO;
import com.alpaca.repository.CustomRepo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Abstract base implementation of {@link IGenericDAO}, providing generic CRUD and pagination
 * operations for any entity type {@code T} with identifier type {@code ID}.
 *
 * <p>Concrete DAO implementations must supply the specific {@link CustomRepo} and entity class by
 * implementing the abstract {@link #getRepo()} method.
 *
 * <p>This class leverages Spring Data repositories to delegate standard persistence operations in a
 * type-safe, reusable fashion.
 *
 * @param <T> entity type managed by this DAO
 * @param <I> type of the entity's identifier
 */
public abstract class GenericDAOImpl<T, I> implements IGenericDAO<T, I> {

    /**
     * Supplies the Spring Data repository for the specific entity type.
     *
     * @return the repository instance for {@code T}
     */
    @Generated
    protected abstract CustomRepo<T, I> getRepo();

    /**
     * Finds an entity by its identifier.
     *
     * @param i the identifier of the entity to find; may be {@code null}
     * @return an {@link Optional} containing the entity if found, otherwise empty
     */
    @Override
    public Optional<T> findById(I i) {
        return getRepo().findById(i);
    }

    /**
     * Retrieves all entities matching the provided collection of identifiers.
     *
     * @param is the collection of identifiers; may be {@code null} or empty
     * @return a {@link List} of entities found; empty if none match
     */
    @Override
    public List<T> findAllByIds(Collection<I> is) {
        return getRepo().findAllById(is);
    }

    /**
     * Deletes the entity identified by the given ID, if it exists.
     *
     * @param i the identifier of the entity to delete; may be {@code null}
     */
    @Override
    public void deleteById(I i) {
        getRepo().deleteById(i);
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
     * @param i the identifier to check; may be {@code null}
     * @return {@code true} if an entity exists with the specified ID; {@code false} otherwise
     */
    @Override
    public boolean existsById(I i) {
        return getRepo().existsById(i);
    }

    @Override
    public boolean existsAllByIds(Collection<I> is) {
        return (is.size()) == getRepo().countEntitiesIds(is);
    }
}
