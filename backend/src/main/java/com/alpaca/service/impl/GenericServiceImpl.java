package com.alpaca.service.impl;

import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.service.IGenericService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Abstract base implementation of {@link IGenericService}, providing reusable CRUD operations with
 * standard validations and error handling for any entity type {@code T} and identifier type {@code
 * ID}.
 *
 * <p>Concrete services should extend this class and provide specific {@link IGenericDAO} instances
 * and entity names for exception messages.
 *
 * @param <T> the type of entity managed
 * @param <I> the type of the entity's identifier
 * @see IGenericService
 */
public abstract class GenericServiceImpl<T, I> implements IGenericService<T, I> {

    /**
     * Supplies the DAO component for data access operations.
     *
     * @return the {@link IGenericDAO} corresponding to the entity type {@code T}
     */
    protected abstract IGenericDAO<T, I> getDAO();

    /**
     * Provides a human-readable entity name to be used in exception messages.
     *
     * @return the name of the entity (e.g., "User")
     */
    protected abstract String getEntityName();

    /**
     * Retrieves an entity by its identifier with validation and error handling.
     *
     * @param i the entity identifier to find; must not be {@code null}
     * @return the entity if found
     * @throws BadRequestException if {@code id} is {@code null}
     * @throws NotFoundException if no entity is found for the given {@code id}
     */
    @Transactional
    @Override
    public T findById(I i) {
        if (i == null) {
            throw new BadRequestException(String.format("%s cannot be found", getEntityName()));
        }
        return getDAO().findById(i)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        String.format(
                                                "%s with ID %s not found", getEntityName(), i)));
    }

    /**
     * Fetches all entities matching the provided collection of IDs, with validation and existence
     * check.
     *
     * @param is the collection of identifiers; must not be {@code null}, empty, or contain {@code
     *     null}
     * @return a list of entities matching the provided IDs
     * @throws BadRequestException if validation fails
     * @throws NotFoundException if any of the requested entities are not found
     */
    @Transactional
    @Override
    public List<T> findAllByIds(Collection<I> is) {
        if (invalidCollection(is)) {
            throw new BadRequestException(String.format("%s(s) cannot be found", getEntityName()));
        }
        List<T> will = getDAO().findAllByIds(is);
        if (will.size() != is.size()) {
            throw new NotFoundException(
                    String.format("Some %s(s) cannot be found", getEntityName()));
        }
        return will;
    }

    /**
     * Saves a new entity with validation against duplication.
     *
     * @param t the entity to save; must not be {@code null}
     * @return the saved entity
     * @throws BadRequestException if {@code t} is {@code null} or already exists
     */
    @Transactional
    @Override
    public T save(T t) {
        if (t == null) {
            throw new BadRequestException(String.format("%s cannot be created", getEntityName()));
        }
        if (getDAO().existsByUniqueProperties(t)) {
            throw new BadRequestException(String.format("%s already exists", getEntityName()));
        }
        return getDAO().save(t);
    }

    /**
     * Saves a collection of entities in bulk.
     *
     * @param t the collection of entities; must not be {@code null}, empty, or contain {@code null}
     * @return the list of saved entities
     * @throws BadRequestException if validation fails
     */
    @Transactional
    @Override
    public List<T> saveAll(Collection<T> t) {
        if (invalidCollection(t)) {
            throw new BadRequestException(
                    String.format("%s(s) cannot be created", getEntityName()));
        }
        return getDAO().saveAll(t);
    }

    /**
     * Deletes an entity by its ID with validation of existence.
     *
     * @param i the identifier of the entity to delete; must not be {@code null}
     * @throws BadRequestException if {@code id} is {@code null} or does not exist
     */
    @Transactional
    @Override
    public void deleteById(I i) {
        if (i == null) {
            throw new BadRequestException(String.format("%s cannot be deleted", getEntityName()));
        }
        if (!getDAO().existsById(i)) {
            throw new BadRequestException(String.format("%s not exists", getEntityName()));
        }
        getDAO().deleteById(i);
    }

    /**
     * Retrieves all entities of type {@code T}.
     *
     * @return a list of all entities
     */
    @Transactional
    @Override
    public List<T> findAll() {
        return getDAO().findAll();
    }

    /**
     * Retrieves entities in a paginated fashion.
     *
     * @param pageable pagination parameters; must not be {@code null}
     * @return a paginated list of entities
     * @throws BadRequestException if {@code pageable} is {@code null}
     */
    @Transactional
    @Override
    public Page<T> findAllPage(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException(
                    String.format("%s(s) Page cannot be created", getEntityName()));
        }
        return getDAO().findAllPage(pageable);
    }

    /**
     * Checks whether an entity exists by its ID.
     *
     * @param i the identifier to check; may be {@code null}
     * @return {@code true} if entity exists, {@code false} otherwise
     */
    @Transactional
    @Override
    public boolean existsById(I i) {
        if (i == null) return false;
        return getDAO().existsById(i);
    }

    /**
     * Verifies that all provided IDs correspond to existing entities.
     *
     * @param is the collection of IDs; must not be {@code null}, empty, or contain {@code null}
     * @return {@code true} if all IDs exist; {@code false} otherwise
     */
    @Transactional
    @Override
    public boolean existsAllByIds(Collection<I> is) {
        if (invalidCollection(is)) return false;
        return getDAO().existsAllByIds(is);
    }

    /**
     * Checks for existence of an entity based on unique properties.
     *
     * @param t the entity to check; may be {@code null}
     * @return {@code true} if such an entity exists; {@code false} otherwise
     */
    @Transactional
    @Override
    public boolean existsByUniqueProperties(T t) {
        if (t == null) return false;
        return getDAO().existsByUniqueProperties(t);
    }

    public boolean invalidCollection(Collection<?> is) {
        return is == null || is.isEmpty() || hasNull(is);
    }

    public boolean hasNull(Collection<?> collection) {
        for (Object element : collection) {
            if (element == null) return true;
        }
        return false;
    }

    public <V> void updateIfNotNull(V existing, V incoming, Consumer<V> setter) {
        if (incoming != null && !incoming.equals(existing)) {
            setter.accept(incoming);
        }
    }

    public void updateTextIfExists(String existing, String incoming, Consumer<String> setter) {
        if (StringUtils.hasText(incoming) && !incoming.equals(existing)) {
            setter.accept(incoming);
        }
    }

    public void updateIfDifferent(boolean existing, boolean incoming, Consumer<Boolean> setter) {
        if (!Objects.equals(existing, incoming)) {
            setter.accept(incoming);
        }
    }
}
