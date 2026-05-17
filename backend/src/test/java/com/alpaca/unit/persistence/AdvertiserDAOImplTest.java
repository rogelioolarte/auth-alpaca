package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.resources.AdvertiserProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/** Unit tests for {@link AdvertiserDAOImpl}. */
@ExtendWith(MockitoExtension.class)
class AdvertiserDAOImplTest {

    @Mock private AdvertiserRepo repo;

    @InjectMocks private AdvertiserDAOImpl dao;

    private Advertiser firstEntity;
    private Page<Advertiser> entitiesPage;

    @BeforeEach
    void setup() {
        firstEntity = AdvertiserProvider.singleEntity();
        List<Advertiser> entities = AdvertiserProvider.listEntities();
        entitiesPage = new PageImpl<>(entities);
    }

    @Nested
    @DisplayName("existsByUniqueProperties Logic")
    class ExistsByUniquePropertiesTests {

        @Test
        @DisplayName("Should return false when user or user ID is null")
        void existsByUniqueProperties_InvalidUser() {
            Advertiser advertiser = new Advertiser();

            // Case 1: User is null
            advertiser.setUser(null);
            assertThat(dao.existsByUniqueProperties(advertiser)).isFalse();

            // Case 2: User ID is null
            User user = new User();
            advertiser.setUser(user);
            assertThat(dao.existsByUniqueProperties(advertiser)).isFalse();

            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("Should return true/false based on repo count")
        void existsByUniqueProperties_ValidUser() {
            UUID userId = firstEntity.getUser().getId();

            when(repo.countByUserId(userId)).thenReturn(1L);
            assertThat(dao.existsByUniqueProperties(firstEntity)).isTrue();

            when(repo.countByUserId(userId)).thenReturn(0L);
            assertThat(dao.existsByUniqueProperties(firstEntity)).isFalse();
        }
    }

    @Test
    @DisplayName("findAllPageByIndexedTrue: Should return all entities by indexed true")
    void findAllPageByIndexedTrue_Success() {
        when(repo.findAllPageByIndexedTrue(null)).thenReturn(entitiesPage);
        assertThat(dao.findAllPageByIndexedTrue(null)).isEqualTo(entitiesPage);
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        List<UUID> ids = entitiesPage.getContent().stream().map(Advertiser::getId).toList();
        when(repo.countByIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countByIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
