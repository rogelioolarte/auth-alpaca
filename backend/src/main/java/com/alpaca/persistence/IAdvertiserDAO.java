package com.alpaca.persistence;

import com.alpaca.entity.Advertiser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Data Access Object (DAO) interface for managing {@code Advertiser} entities. Extends {@link
 * IGenericDAO} to inherit common CRUD operations.
 *
 * @see IGenericDAO
 */
public interface IAdvertiserDAO extends IGenericDAO<Advertiser, UUID> {

    Page<Advertiser> findAllPageByIndexedTrue(Pageable pageable);
}
