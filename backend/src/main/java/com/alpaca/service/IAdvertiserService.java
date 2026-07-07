package com.alpaca.service;

import com.alpaca.entity.Advertiser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for managing {@link Advertiser} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IAdvertiserService extends IGenericService<Advertiser, UUID> {

    /**
     * Retrieves a paginated list of advertisers that are marked as indexed.
     *
     * @param pageable the pagination configuration — must not be null
     * @return a {@code Page} containing the indexed advertisers
     */
    Page<Advertiser> findAllByIndexedTrue(Pageable pageable);
}
