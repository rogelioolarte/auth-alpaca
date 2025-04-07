package com.alpaca.persistence;

import com.alpaca.entity.Role;

import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link Role} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code Role} management.
 *
 * @see IGenericDAO
 */
public interface IRoleDAO extends IGenericDAO<Role, UUID> {

  /**
   * Finds a role by its unique name.
   *
   * @param roleName The name of the role - must not be null.
   * @return An {@link Optional} containing the role if found, otherwise an empty {@link Optional}.
   */
  Optional<Role> findByRoleName(String roleName);
}
