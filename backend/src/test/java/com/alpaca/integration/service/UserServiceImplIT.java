package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UserServiceImpl}.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class UserServiceImplIT {

	@Autowired private UserServiceImpl service;

	private User firstTemplate;
	private User alternativeTemplate;

	@BeforeEach
	void setup() {
		firstTemplate = UserProvider.singleTemplate();
		alternativeTemplate = UserProvider.alternativeTemplate();
	}

	// --- register ---

	@Test
	@Transactional
	void register_whenUserIsNull_thenThrowBadRequest() {
		assertThrows(BadRequestException.class, () -> service.register(null));
	}

	@Test
	@Transactional
	void register_whenValidUser_thenPersistAndReturn() {
		User toRegister = new User(
				firstTemplate.getEmail(), firstTemplate.getPassword(), new HashSet<>());
		User saved = service.register(toRegister);

		assertNotNull(saved);
		assertNotNull(saved.getId());
		assertEquals(firstTemplate.getEmail(), saved.getEmail());

		// findByEmail should now return the persisted user
		User found = service.findByEmail(firstTemplate.getEmail());
		assertNotNull(found);
		assertEquals(saved.getId(), found.getId());
		assertEquals(saved.getEmail(), found.getEmail());
	}

	// --- existsByEmail ---

	@Test
	@Transactional
	void existsByEmail_whenNoUser_thenFalse() {
		// alternativeTemplate email should not exist yet
		assertFalse(service.existsByEmail(alternativeTemplate.getEmail()));
	}

	@Test
	@Transactional
	void existsByEmail_whenUserExists_thenTrue() {
		User toRegister = new User(
				firstTemplate.getEmail(), firstTemplate.getPassword(), new HashSet<>());
		service.register(toRegister);

		assertTrue(service.existsByEmail(firstTemplate.getEmail()));
	}

	// --- findByEmail ---

	@Test
	@Transactional
	void findByEmail_whenEmailIsNull_thenThrowBadRequest() {
		assertThrows(BadRequestException.class, () -> service.findByEmail(null));
	}

	@Test
	@Transactional
	void findByEmail_whenEmailIsBlank_thenThrowBadRequest() {
		assertThrows(BadRequestException.class, () -> service.findByEmail("   "));
	}

	@Test
	@Transactional
	void findByEmail_whenNotFound_thenThrowUsernameNotFoundException() {
		// alternativeTemplate email not persisted -> should throw UsernameNotFoundException
		assertThrows(
				UsernameNotFoundException.class,
				() -> service.findByEmail(alternativeTemplate.getEmail()));
	}

	@Test
	@Transactional
	void findByEmail_whenExists_thenReturnUser() {
		User toRegister = new User(
				firstTemplate.getEmail(), firstTemplate.getPassword(), new HashSet<>());
		User saved = service.register(toRegister);

		User found = service.findByEmail(firstTemplate.getEmail());
		assertNotNull(found);
		assertEquals(saved.getId(), found.getId());
		assertEquals(saved.getEmail(), found.getEmail());
	}
}