package com.alpaca.repository;

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
public interface GenericRepo<T, I> extends JpaRepository<T, I> {}
