package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.ProfileDAOImpl;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.resources.ProfileProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileDAOImplTest {

  @Mock private ProfileRepo repo;

  @InjectMocks private ProfileDAOImpl dao;

  private Profile firstEntity;
  private Profile secondEntity;
  private Profile thirdEntity;

  @BeforeEach
  void setup() {
    firstEntity = ProfileProvider.singleEntity();
    secondEntity = ProfileProvider.alternativeEntity();
    thirdEntity = ProfileProvider.alternativeEntity();
  }

  @Test
  void updateByIdCaseOne() {
    when(repo.findById(firstEntity.getId())).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> dao.updateById(firstEntity, firstEntity.getId()));
    verify(repo).findById(firstEntity.getId());
  }

  @Test
  void updateByIdCaseTwo() {
    Profile newEntitySecond = new Profile();
    newEntitySecond.setAddress(null);
    newEntitySecond.setAvatarUrl(null);
    newEntitySecond.setUser(null);
    newEntitySecond.setLastName(null);
    newEntitySecond.setFirstName(null);
    when(repo.findById(secondEntity.getId())).thenReturn(Optional.of(secondEntity));
    when(repo.save(secondEntity)).thenReturn(secondEntity);
    Profile updatedEntity = dao.updateById(newEntitySecond, secondEntity.getId());
    assertNotNull(updatedEntity);
    assertEquals(secondEntity.getId(), updatedEntity.getId());
    assertNotEquals(newEntitySecond.getAddress(), updatedEntity.getAddress());
    verify(repo).findById(secondEntity.getId());
    verify(repo).save(secondEntity);
  }

  @Test
  void updateByIdCaseThree() {
    Profile newEntityThird = new Profile();
    newEntityThird.setAddress(" ");
    newEntityThird.setLastName(" ");
    newEntityThird.setFirstName(" ");
    newEntityThird.setAvatarUrl(" ");
    User newUserThird = new User();
    newUserThird.setId(null);
    newEntityThird.setUser(newUserThird);
    when(repo.findById(thirdEntity.getId())).thenReturn(Optional.of(thirdEntity));
    when(repo.save(thirdEntity)).thenReturn(thirdEntity);
    Profile updatedEntity = dao.updateById(newEntityThird, thirdEntity.getId());
    assertNotNull(updatedEntity);
    assertEquals(thirdEntity.getId(), updatedEntity.getId());
    assertNotEquals(newEntityThird.getAddress(), updatedEntity.getAddress());
    verify(repo).findById(thirdEntity.getId());
    verify(repo).save(thirdEntity);
  }

  @Test
  void updateByIdCaseFour() {
    when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(firstEntity));
    when(repo.save(firstEntity)).thenReturn(firstEntity);
    Profile updatedEntity = dao.updateById(secondEntity, firstEntity.getId());
    assertNotNull(updatedEntity);
    assertEquals(firstEntity.getId(), updatedEntity.getId());
    assertEquals(secondEntity.getAddress(), updatedEntity.getAddress());
    assertNotEquals(secondEntity.getId(), updatedEntity.getId());
    verify(repo).findById(firstEntity.getId());
    verify(repo).save(firstEntity);
  }

  @Test
  void existsByUniquePropertiesCaseOne() {
    Profile entityWithNullUser = new Profile();
    entityWithNullUser.setUser(null);
    assertFalse(dao.existsByUniqueProperties(entityWithNullUser));
  }

  @Test
  void existsByUniquePropertiesCaseTwo() {
    Profile entityWithNullUserId = new Profile();
    User userWithNullId = new User();
    userWithNullId.setId(null);
    entityWithNullUserId.setUser(userWithNullId);
    assertFalse(dao.existsByUniqueProperties(entityWithNullUserId));
  }

  @Test
  void existsByUniquePropertiesCaseThree() {
    when(repo.countByUserId(secondEntity.getUser().getId())).thenReturn(0L);
    assertFalse(dao.existsByUniqueProperties(secondEntity));
    verify(repo).countByUserId(secondEntity.getUser().getId());
  }

  @Test
  void existsByUniquePropertiesCaseFour() {
    when(repo.countByUserId(firstEntity.getUser().getId())).thenReturn(1L);
    assertTrue(dao.existsByUniqueProperties(firstEntity));
    verify(repo).countByUserId(firstEntity.getUser().getId());
  }
}
