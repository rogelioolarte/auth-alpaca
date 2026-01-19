package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Unit tests for {@link UserServiceImpl} */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserDAOImpl dao;

    @InjectMocks private UserServiceImpl service;

    private User firstEntity;
    private User secondEntity;

    @BeforeEach
    void setup() {
        firstEntity = UserProvider.singleEntity();
        secondEntity = UserProvider.alternativeEntity();
    }

    // --- register ---
    @Test
    void registerCaseOne() {
        assertThrows(BadRequestException.class, () -> service.register(null));
    }

    @Test
    void registerCaseTwo() {
        when(dao.save(secondEntity)).thenReturn(null);
        assertNull(service.register(secondEntity));
        verify(dao).save(secondEntity);
    }

    @Test
    void registerCaseThree() {
        when(dao.save(firstEntity)).thenReturn(firstEntity);
        User entityFound = service.register(firstEntity);
        assertEquals(firstEntity, entityFound);
        verify(dao).save(firstEntity);
    }

    // --- existsByEmail ---
    @Test
    void existsByEmailCaseOne() {
        when(dao.existsByEmail(secondEntity.getEmail())).thenReturn(false);
        assertFalse(service.existsByEmail(secondEntity.getEmail()));
        verify(dao).existsByEmail(secondEntity.getEmail());
    }

    @Test
    void existsByEmailCaseTwo() {
        when(dao.existsByEmail(firstEntity.getEmail())).thenReturn(true);
        assertTrue(service.existsByEmail(firstEntity.getEmail()));
        verify(dao).existsByEmail(firstEntity.getEmail());
    }

    // --- findByEmail ---
    @Test
    void findByEmailCaseOne() {
        assertThrows(BadRequestException.class, () -> service.findByEmail(null));
    }

    @Test
    void findByEmailCaseTwo() {
        assertThrows(BadRequestException.class, () -> service.findByEmail("  "));
    }

    @Test
    void findByEmailCaseThree() {
        when(dao.findByEmailWithAuthorities(secondEntity.getEmail())).thenReturn(Optional.empty());
        assertThrows(
                UsernameNotFoundException.class,
                () -> service.findByEmail(secondEntity.getEmail()));
        verify(dao).findByEmailWithAuthorities(secondEntity.getEmail());
    }

    @Test
    void findByEmailCaseFour() {
        when(dao.findByEmailWithAuthorities(firstEntity.getEmail()))
                .thenReturn(Optional.ofNullable(firstEntity));
        User entityFound = service.findByEmail(firstEntity.getEmail());
        assertEquals(firstEntity, entityFound);
        verify(dao).findByEmailWithAuthorities(firstEntity.getEmail());
    }

    // --- isAllowUser ---
    @Test
    void isAllowUserCaseOne() {
        assertTrue(firstEntity.isAllowUser());
    }

    @Test
    void isAllowUserCaseTwo() {
        assertFalse(UserProvider.notAllowEntity().isAllowUser());
    }

    // --- getAuthorities ---
    @Test
    void getAuthoritiesCaseOne() {
        assertTrue(firstEntity.getAuthorities().isEmpty());
    }

    @Test
    void getAuthoritiesCaseTwo() {
        User userWithRoles = UserProvider.singleEntity();
        userWithRoles.setUserRoles(new HashSet<>(Set.of(RoleProvider.singleEntity())));
        assertFalse(userWithRoles.getAuthorities().isEmpty());
    }
}
