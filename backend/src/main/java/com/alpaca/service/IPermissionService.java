package com.alpaca.service;

import com.alpaca.entity.Permission;

import java.util.UUID;

/**
 * Service interface for managing {@code Permission} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IPermissionService extends IGenericService<Permission, UUID> {}
