package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.resources.provider.AdvertiserProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.service.impl.AdvertiserServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Unit tests for {@link AdvertiserServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class AdvertiserServiceImplTest {

    @Mock private IAdvertiserDAO dao;

    @InjectMocks private AdvertiserServiceImpl service;

    private Advertiser firstAdvertiser;
    private Advertiser secondAdvertiser;

    @BeforeEach
    void setup() {
        firstAdvertiser = AdvertiserProvider.singleEntity();
        secondAdvertiser = AdvertiserProvider.alternativeEntity();
    }

    // --- save ---

    @Test
    void saveShouldThrowBadRequestExceptionWhenAdvertiserIsNull() {
        assertThrows(BadRequestException.class, () -> service.save(null));

        verify(dao).existsByUniqueProperties(null);
    }

    @Test
    void saveShouldThrowBadRequestExceptionWhenAdvertiserAlreadyExists() {
        when(dao.existsByUniqueProperties(firstAdvertiser)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.save(firstAdvertiser));

        verify(dao).existsByUniqueProperties(firstAdvertiser);
        verify(dao, never()).save(any(Advertiser.class));
    }

    @Test
    void saveShouldPersistAdvertiserSuccessfully() {
        when(dao.save(secondAdvertiser)).thenReturn(secondAdvertiser);

        Advertiser result = service.save(secondAdvertiser);

        assertNotNull(result);
        assertEquals(secondAdvertiser, result);

        verify(dao).save(secondAdvertiser);
    }

    // --- findAllPageByIndexedTrue ---

    @Test
    void findAllPageByIndexedTrueShouldReturnPagedAdvertisersSuccessfully() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Advertiser> expectedPage = new PageImpl<>(List.of(firstAdvertiser, secondAdvertiser));

        when(dao.findAllPageByIndexedTrue(pageable)).thenReturn(expectedPage);

        Page<Advertiser> result = service.findAllPageByIndexedTrue(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage, result);

        verify(dao).findAllPageByIndexedTrue(pageable);
    }

    // --- updateById ---

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenAdvertiserIsNull() {
        UUID advertiserId = firstAdvertiser.getId();

        assertThrows(BadRequestException.class, () -> service.updateById(null, advertiserId));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(firstAdvertiser, null));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowNotFoundExceptionWhenAdvertiserDoesNotExist() {
        UUID advertiserId = firstAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class, () -> service.updateById(firstAdvertiser, advertiserId));

        verify(dao).findById(advertiserId);
        verify(dao, never()).save(any(Advertiser.class));
    }

    @Test
    void updateByIdShouldUpdateAllFieldsSuccessfully() {
        Advertiser existingAdvertiser = AdvertiserProvider.singleEntity();

        Advertiser incomingAdvertiser = AdvertiserProvider.alternativeEntity();

        UUID advertiserId = existingAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.of(existingAdvertiser));
        when(dao.save(existingAdvertiser)).thenReturn(existingAdvertiser);

        Advertiser result = service.updateById(incomingAdvertiser, advertiserId);

        assertNotNull(result);
        assertEquals(incomingAdvertiser.getTitle(), result.getTitle());
        assertEquals(incomingAdvertiser.getDescription(), result.getDescription());
        assertEquals(incomingAdvertiser.getAvatarUrl(), result.getAvatarUrl());
        assertEquals(incomingAdvertiser.getBannerUrl(), result.getBannerUrl());
        assertEquals(incomingAdvertiser.getPublicLocation(), result.getPublicLocation());
        assertEquals(incomingAdvertiser.getPublicUrlLocation(), result.getPublicUrlLocation());
        assertEquals(incomingAdvertiser.isIndexed(), result.isIndexed());
        assertEquals(incomingAdvertiser.getUser(), result.getUser());

        ArgumentCaptor<Advertiser> advertiserCaptor = ArgumentCaptor.forClass(Advertiser.class);

        verify(dao).save(advertiserCaptor.capture());

        Advertiser savedAdvertiser = advertiserCaptor.getValue();

        assertEquals(incomingAdvertiser.getTitle(), savedAdvertiser.getTitle());
        assertEquals(incomingAdvertiser.getDescription(), savedAdvertiser.getDescription());
        assertEquals(incomingAdvertiser.getAvatarUrl(), savedAdvertiser.getAvatarUrl());
        assertEquals(incomingAdvertiser.getBannerUrl(), savedAdvertiser.getBannerUrl());
        assertEquals(incomingAdvertiser.getPublicLocation(), savedAdvertiser.getPublicLocation());
        assertEquals(
                incomingAdvertiser.getPublicUrlLocation(), savedAdvertiser.getPublicUrlLocation());
        assertEquals(incomingAdvertiser.isIndexed(), savedAdvertiser.isIndexed());
        assertEquals(incomingAdvertiser.getUser(), savedAdvertiser.getUser());
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserIsNull() {
        Advertiser existingAdvertiser = AdvertiserProvider.singleEntity();

        User originalUser = existingAdvertiser.getUser();

        Advertiser incomingAdvertiser = AdvertiserProvider.alternativeEntity();
        incomingAdvertiser.setUser(null);

        UUID advertiserId = existingAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.of(existingAdvertiser));
        when(dao.save(existingAdvertiser)).thenReturn(existingAdvertiser);

        Advertiser result = service.updateById(incomingAdvertiser, advertiserId);

        assertNotNull(result);
        assertEquals(originalUser, result.getUser());

        verify(dao).save(existingAdvertiser);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserIdIsNull() {
        Advertiser existingAdvertiser = AdvertiserProvider.singleEntity();

        User originalUser = existingAdvertiser.getUser();

        Advertiser incomingAdvertiser = AdvertiserProvider.alternativeEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setId(null);

        incomingAdvertiser.setUser(incomingUser);

        UUID advertiserId = existingAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.of(existingAdvertiser));
        when(dao.save(existingAdvertiser)).thenReturn(existingAdvertiser);

        Advertiser result = service.updateById(incomingAdvertiser, advertiserId);

        assertNotNull(result);
        assertEquals(originalUser, result.getUser());

        verify(dao).save(existingAdvertiser);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenUserIdsAreEqual() {
        Advertiser existingAdvertiser = AdvertiserProvider.singleEntity();

        User existingUser = existingAdvertiser.getUser();

        Advertiser incomingAdvertiser = AdvertiserProvider.alternativeEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setId(existingUser.getId());

        incomingAdvertiser.setUser(incomingUser);

        UUID advertiserId = existingAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.of(existingAdvertiser));
        when(dao.save(existingAdvertiser)).thenReturn(existingAdvertiser);

        Advertiser result = service.updateById(incomingAdvertiser, advertiserId);

        assertNotNull(result);
        assertEquals(existingUser, result.getUser());

        verify(dao).save(existingAdvertiser);
    }

    @Test
    void updateByIdShouldUpdateUserWhenExistingUserIsNull() {
        Advertiser existingAdvertiser = AdvertiserProvider.singleEntity();
        existingAdvertiser.setUser(null);

        Advertiser incomingAdvertiser = AdvertiserProvider.alternativeEntity();

        UUID advertiserId = existingAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.of(existingAdvertiser));
        when(dao.save(existingAdvertiser)).thenReturn(existingAdvertiser);

        Advertiser result = service.updateById(incomingAdvertiser, advertiserId);

        assertNotNull(result);
        assertEquals(incomingAdvertiser.getUser(), result.getUser());

        verify(dao).save(existingAdvertiser);
    }

    @Test
    void updateByIdShouldNotUpdateBlankTextFields() {
        Advertiser existingAdvertiser = AdvertiserProvider.singleEntity();

        String originalTitle = existingAdvertiser.getTitle();
        String originalDescription = existingAdvertiser.getDescription();
        String originalAvatarUrl = existingAdvertiser.getAvatarUrl();
        String originalBannerUrl = existingAdvertiser.getBannerUrl();
        String originalPublicLocation = existingAdvertiser.getPublicLocation();
        String originalPublicUrlLocation = existingAdvertiser.getPublicUrlLocation();

        Advertiser incomingAdvertiser = AdvertiserProvider.alternativeEntity();
        incomingAdvertiser.setTitle(" ");
        incomingAdvertiser.setDescription(" ");
        incomingAdvertiser.setAvatarUrl(" ");
        incomingAdvertiser.setBannerUrl(" ");
        incomingAdvertiser.setPublicLocation(" ");
        incomingAdvertiser.setPublicUrlLocation(" ");

        incomingAdvertiser.setUser(existingAdvertiser.getUser());

        UUID advertiserId = existingAdvertiser.getId();

        when(dao.findById(advertiserId)).thenReturn(Optional.of(existingAdvertiser));
        when(dao.save(existingAdvertiser)).thenReturn(existingAdvertiser);

        Advertiser result = service.updateById(incomingAdvertiser, advertiserId);

        assertNotNull(result);
        assertEquals(originalTitle, result.getTitle());
        assertEquals(originalDescription, result.getDescription());
        assertEquals(originalAvatarUrl, result.getAvatarUrl());
        assertEquals(originalBannerUrl, result.getBannerUrl());
        assertEquals(originalPublicLocation, result.getPublicLocation());
        assertEquals(originalPublicUrlLocation, result.getPublicUrlLocation());

        verify(dao).save(existingAdvertiser);
    }
}
