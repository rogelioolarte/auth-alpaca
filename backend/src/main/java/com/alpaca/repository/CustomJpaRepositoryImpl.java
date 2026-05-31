package com.alpaca.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

public class CustomJpaRepositoryImpl<T, I> extends SimpleJpaRepository<T, I>
        implements CustomRepo<T, I> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, I> entityInformation;

    public CustomJpaRepositoryImpl(
            JpaEntityInformation<T, I> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    @Override
    @Transactional
    public void deleteById(@NonNull I id) {
        String entityName = entityInformation.getEntityName();

        SingularAttribute<? super T, ?> idAttribute = entityInformation.getIdAttribute();
        if (idAttribute == null) {
            throw new IllegalStateException(
                    String.format("No valid entity %s exists.", entityName));
        }
        String idAttributeName = idAttribute.getName();

        String queryString =
                String.format("DELETE FROM %s e WHERE e.%s = :id", entityName, idAttributeName);

        Query query = entityManager.createQuery(queryString);
        query.setParameter("id", id);

        int affectedRows = query.executeUpdate();
        if (affectedRows == 0) {
            throw new EmptyResultDataAccessException(
                    String.format(
                            "%s with id %s was not found or has already been removed.",
                            entityName, id),
                    1);
        }
    }

    @Override
    public long countEntitiesIds(Collection<I> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }

        String entityName = entityInformation.getEntityName();
        SingularAttribute<? super T, ?> idAttribute = entityInformation.getIdAttribute();
        if (idAttribute == null) {
            throw new IllegalStateException(
                    String.format("No valid entity %s exists.", entityName));
        }
        String idAttributeName = idAttribute.getName();

        String queryString =
                String.format(
                        "SELECT COUNT(e) FROM %s e WHERE e.%s IN :ids",
                        entityName, idAttributeName);

        Query query = entityManager.createQuery(queryString);
        query.setParameter("ids", ids);
        return (long) query.getSingleResult();
    }
}
