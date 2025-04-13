package com.alpaca.repository;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

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
 * @param <ID> The primary key type of the entity.
 * @see JpaRepository
 */
@NoRepositoryBean
public interface GenericRepo<T, ID> extends JpaRepository<T, ID> {

  /**
   * Counts the number of entities with the given IDs.
   *
   * @param ids The collection of entity IDs to count - must not be null.
   * @return The number of entities found matching the provided IDs.
   */
  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.id IN :ids")
  long countByIds(@Param("ids") Collection<ID> ids);
}
