package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserDAOImpl dao;

  @InjectMocks private UserServiceImpl service;

  @Test
  void register() {
    assertThrows(BadRequestException.class, () -> service.register(null));

    User entitySecond = UserProvider.alternativeEntity();
    when(dao.save(entitySecond)).thenReturn(null);
    assertNull(service.register(entitySecond));
    verify(dao).save(entitySecond);

    User entity = UserProvider.singleEntity();
    when(dao.save(entity)).thenReturn(entity);
    User entityFound = service.register(entity);
    assertEquals(entity, entityFound);
    verify(dao).save(entity);
  }

  @Test
  void existsByEmail() {
    User alternativeEntity = UserProvider.alternativeEntity();
    when(dao.existsByEmail(alternativeEntity.getEmail())).thenReturn(false);
    assertFalse(service.existsByEmail(alternativeEntity.getEmail()));
    verify(dao).existsByEmail(alternativeEntity.getEmail());

    User entity = UserProvider.singleEntity();
    when(dao.existsByEmail(entity.getEmail())).thenReturn(true);
    assertTrue(service.existsByEmail(entity.getEmail()));
    verify(dao).existsByEmail(entity.getEmail());
  }

  @Test
  void findByEmail() {
    assertThrows(BadRequestException.class, () -> service.findByEmail(null));

    assertThrows(BadRequestException.class, () -> service.findByEmail("  "));

    User entitySecond = UserProvider.alternativeEntity();
    when(dao.findByEmail(entitySecond.getEmail())).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.findByEmail(entitySecond.getEmail()));
    verify(dao).findByEmail(entitySecond.getEmail());

    User entity = UserProvider.singleEntity();
    when(dao.findByEmail(entity.getEmail())).thenReturn(Optional.of(entity));
    User entityFound = service.findByEmail(entity.getEmail());
    assertEquals(entityFound, entity);
    verify(dao).findByEmail(entity.getEmail());
  }
}
