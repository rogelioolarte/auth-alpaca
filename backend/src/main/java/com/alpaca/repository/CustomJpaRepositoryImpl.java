package com.alpaca.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Custom base implementation of {@link SimpleJpaRepository} that overrides the default {@link
 * #deleteById(Object)} behavior and provides a batch-count query.
 *
 * <p>The standard Spring Data {@code deleteById} silently succeeds when the entity does not exist.
 * This implementation replaces that behavior with an explicit {@link
 * EmptyResultDataAccessException} when no matching row is found. The {@link
 * #countEntitiesIds(Collection)} method provides a single-query batch existence check instead of
 * loading full entity proxies.
 *
 * @param <T> entity type
 * @param <I> entity identifier type
 */
public class CustomJpaRepositoryImpl<T, I> extends SimpleJpaRepository<T, I>
        implements CustomRepo<T, I> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, I> entityInformation;

    /**
     * Creates a custom repository implementation for the given entity type.
     *
     * @param entityInformation metadata about the entity type, used for JPQL query construction
     * @param entityManager the JPA {@link EntityManager} for executing queries
     */
    public CustomJpaRepositoryImpl(
            JpaEntityInformation<T, I> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    /**
     * Deletes an entity via a bulk JPQL DELETE statement, throwing {@link
     * EmptyResultDataAccessException} when no row is affected.
     *
     * <p>This differs from the default {@code SimpleJpaRepository.deleteById()} which calls {@code
     * findById()} first and silently returns if the entity is not found. Here, the entity is
     * deleted in a single JPQL statement and the caller is explicitly notified if nothing was
     * deleted.
     */
    @Override
    @Transactional
    public void deleteById(@NonNull I id) {
        Class<T> entityClass = entityInformation.getJavaType();
        String idAttributeName = resolveIdAttributeName();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> delete = cb.createCriteriaDelete(entityClass);
        Root<T> root = delete.from(entityClass);
        delete.where(cb.equal(root.get(idAttributeName), id));

        int affectedRows = entityManager.createQuery(delete).executeUpdate();
        if (affectedRows == 0) {
            throw new EmptyResultDataAccessException(
                    String.format(
                            "%s with id %s was not found or has already been removed.",
                            entityInformation.getEntityName(), id),
                    1);
        }
    }

    /**
     * Counts how many of the provided entity IDs actually exist in the database.
     *
     * <p>This is used to implement batch existence checks without loading entity proxies. It
     * executes a single {@code SELECT COUNT} query with an {@code IN} clause and returns the count
     * of matching rows. Returns zero for a {@code null} or empty collection.
     */
    @Override
    public long countEntitiesIds(Collection<I> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }

        Class<T> entityClass = entityInformation.getJavaType();
        String idAttributeName = resolveIdAttributeName();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        query.select(cb.count(root));
        query.where(root.get(idAttributeName).in(ids));

        return entityManager.createQuery(query).getSingleResult();
    }

    private String resolveIdAttributeName() {
        var idAttribute = entityInformation.getIdAttribute();
        if (idAttribute == null) {
            throw new IllegalStateException(
                    String.format("No valid id attribute for entity %s.",
                            entityInformation.getEntityName()));
        }
        return idAttribute.getName();
    }
}
