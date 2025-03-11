package com.alpaca.persistence.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.GenericRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdvertiserDAOImpl extends GenericDAOImpl<Advertiser, UUID> implements IAdvertiserDAO {

    private final AdvertiserRepo repo;

    @Override
    protected GenericRepo<Advertiser, UUID> getRepo() {
        return repo;
    }

    @Override
    protected Class<Advertiser> getEntity() {
        return Advertiser.class;
    }

    @Override
    public Advertiser updateById(Advertiser advertiser, UUID id) {
        Advertiser existingAdvertiser = findById(id).orElseThrow(() ->
                new NotFoundException(String.format("%s with ID %s not found",
                        getEntity().getName(), id.toString())));
        if(advertiser.getTitle() != null && !advertiser.getTitle().isBlank()){
            existingAdvertiser.setTitle(advertiser.getTitle());
        }
        if(advertiser.getDescription() != null && !advertiser.getDescription().isBlank()) {
            existingAdvertiser.setDescription(advertiser.getDescription());
        }
        if(advertiser.getAvatarUrl() != null && !advertiser.getAvatarUrl().isBlank()) {
            existingAdvertiser.setAvatarUrl(advertiser.getAvatarUrl());
        }
        if(advertiser.getBannerUrl() != null && !advertiser.getBannerUrl().isBlank()) {
            existingAdvertiser.setBannerUrl(advertiser.getBannerUrl());
        }
        if(advertiser.getPublicLocation() != null &&
                !advertiser.getPublicLocation().isBlank()) {
            existingAdvertiser.setPublicLocation(advertiser.getPublicLocation());
        }
        if(advertiser.getPublicUrlLocation() != null &&
                !advertiser.getPublicUrlLocation().isBlank()) {
            existingAdvertiser.setPublicUrlLocation(advertiser.getPublicUrlLocation());
        }
        if(advertiser.isIndexed() != existingAdvertiser.isIndexed()) {
            existingAdvertiser.setIndexed(advertiser.isIndexed());
        }
        if(advertiser.getUser() != null && advertiser.getUser().getId() != null) {
            existingAdvertiser.setUser(advertiser.getUser());
        }
        return repo.save(existingAdvertiser);
    }

    @Override
    public boolean existsByUniqueProperties(Advertiser advertiser) {
        if (advertiser.getUser() == null || advertiser.getUser().getId() == null) return false;
        return repo.countByUserId(advertiser.getUser().getId()) > 0L;
    }
}
