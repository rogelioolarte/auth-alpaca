package com.alpaca.persistence;

import com.alpaca.entity.Permission;

import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@code Permission} entities. Extends {@link
 * IGenericDAO} to inherit common CRUD operations.
 *
 * @see IGenericDAO
 */
public interface IPermissionDAO extends IGenericDAO<Permission, UUID> {

  Optional<Permission> findByPermissionName(String permissionName);
}
