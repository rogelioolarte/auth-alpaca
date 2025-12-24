package com.alpaca.service;

import com.alpaca.entity.Advertiser;

import java.util.UUID;

/**
 * Service interface for managing {@link Advertiser} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IAdvertiserService extends IGenericService<Advertiser, UUID> {}
