package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.resources.AdvertiserProvider;
import java.util.List;
import java.util.Optional;
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
    @DisplayName("updateById Logic")
    class UpdateByIdTests {

        @Test
        @DisplayName("Should throw NotFoundException when ID is not found")
        void updateById_NotFound() {
            UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> dao.updateById(new Advertiser(), id));
        }

        @Test
        @DisplayName("Should update all text and boolean fields when they are different and valid")
        void updateById_SuccessfulFullUpdate() {
            UUID id = firstEntity.getId();
            Advertiser incoming = new Advertiser();
            incoming.setTitle("New Title");
            incoming.setDescription("New Desc");
            incoming.setAvatarUrl("https://new-avatar.com");
            incoming.setBannerUrl("https://new-banner.com");
            incoming.setPublicLocation("New Location");
            incoming.setPublicUrlLocation("https://location.com");
            incoming.setIndexed(!firstEntity.isIndexed());

            // User matching ID to trigger user update
            User matchingUser = new User();
            matchingUser.setId(firstEntity.getUser().getId());
            incoming.setUser(matchingUser);

            when(repo.findById(id)).thenReturn(Optional.of(firstEntity));
            when(repo.save(any(Advertiser.class))).thenAnswer(i -> i.getArguments()[0]);

            Advertiser result = dao.updateById(incoming, id);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.isIndexed()).isEqualTo(incoming.isIndexed());
            assertThat(result.getUser().getId()).isEqualTo(matchingUser.getId());
            verify(repo).save(firstEntity);
        }

        @Test
        @DisplayName("Should NOT update fields when incoming values are null, blank or identical")
        void updateById_NoChanges() {
            UUID id = firstEntity.getId();
            String originalTitle = firstEntity.getTitle();

            Advertiser incoming = new Advertiser();
            incoming.setTitle("   "); // Blank
            incoming.setDescription(null); // Null
            incoming.setIndexed(firstEntity.isIndexed()); // Same

            when(repo.findById(id)).thenReturn(Optional.of(firstEntity));
            when(repo.save(firstEntity)).thenReturn(firstEntity);

            Advertiser result = dao.updateById(incoming, id);

            assertThat(result.getTitle()).isEqualTo(originalTitle);
            verify(repo).save(firstEntity);
        }

        @Test
        @DisplayName("User Update: Should cover all branches of the user comparison")
        void updateById_UserBranches() {
            UUID id = firstEntity.getId();
            UUID userId = firstEntity.getUser().getId();
            when(repo.findById(id)).thenReturn(Optional.of(firstEntity));
            when(repo.save(any(Advertiser.class))).thenAnswer(i -> i.getArguments()[0]);

            // Branch 1: advertiser.getUser() is null
            Advertiser incomingNullUser = new Advertiser();
            incomingNullUser.setUser(null);
            dao.updateById(incomingNullUser, id);

            // Branch 2: user is not null but ID is null
            Advertiser incomingNullUserId = new Advertiser();
            User userNoId = new User();
            incomingNullUserId.setUser(userNoId);
            dao.updateById(incomingNullUserId, id);

            // Branch 3: User IDs do not match (Should not update)
            Advertiser incomingDiffUser = new Advertiser();
            User diffUser = new User();
            diffUser.setId(UUID.randomUUID());
            incomingDiffUser.setUser(diffUser);
            dao.updateById(incomingDiffUser, id);

            assertThat(userId).isNotEqualTo(diffUser.getId());
        }
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
