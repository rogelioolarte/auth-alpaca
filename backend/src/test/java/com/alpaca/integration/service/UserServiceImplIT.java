package com.alpaca.integration.service;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link UserServiceImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class UserServiceImplIT {

    @Autowired
    private UserServiceImpl service;

    private User firstEntity;
    private User secondEntity;

    @BeforeEach
    void setup() {
        firstEntity = UserProvider.singleEntity();
        secondEntity = UserProvider.alternativeEntity();
    }

    // --- register ---
    @Test
    @Transactional
    void registerCaseOne() {
        assertThrows(BadRequestException.class, () -> service.register(null));
    }

    @Test
    @Transactional
    void registerCaseTwo() {
        assertNotNull(service.register(new User(firstEntity.getEmail(),
                firstEntity.getPassword(), new HashSet<>())));
    }

    @Test
    @Transactional
    void registerCaseThree() {
        assertEquals(firstEntity.getEmail(), service.register(new User(firstEntity.getEmail(),
                firstEntity.getPassword(), new HashSet<>())).getEmail());
        User persisted = service.findByEmail(firstEntity.getEmail());
        assertNotNull(persisted);
        assertEquals(firstEntity.getEmail(), persisted.getEmail());
    }

    // --- existsByEmail ---
    @Test
    @Transactional
    void existsByEmailCaseOne() {
        assertFalse(service.existsByEmail(secondEntity.getEmail()));
    }

    @Test
    @Transactional
    void existsByEmailCaseTwo() {
        service.save(new User(firstEntity.getEmail(),
                firstEntity.getPassword(), new HashSet<>()));
        assertTrue(service.existsByEmail(firstEntity.getEmail()));
    }

    // --- findByEmail ---
    @Test
    @Transactional
    void findByEmailCaseOne() {
        assertThrows(BadRequestException.class, () -> service.findByEmail(null));
    }

    @Test
    @Transactional
    void findByEmailCaseTwo() {
        assertThrows(BadRequestException.class, () -> service.findByEmail("  "));
    }

    @Test
    @Transactional
    void findByEmailCaseThree() {
        assertThrows(NotFoundException.class, () -> service.findByEmail(secondEntity.getEmail()));
    }

    @Test
    @Transactional
    void findByEmailCaseFour() {
        service.save(new User(firstEntity.getEmail(),
                firstEntity.getPassword(), new HashSet<>()));
        User found = service.findByEmail(firstEntity.getEmail());
        assertEquals(firstEntity.getEmail(), found.getEmail());
    }
}