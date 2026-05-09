package com.alpaca.service;

import com.alpaca.entity.Advertiser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing {@link Advertiser} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IAdvertiserService extends IGenericService<Advertiser, UUID> {

    Page<Advertiser> findAllPageByIndexedTrue(Pageable pageable);
}
