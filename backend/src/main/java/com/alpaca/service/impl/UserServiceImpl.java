package com.alpaca.service.impl;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IUserService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer implementation for managing {@link User} entities and encapsulating business logic
 * beyond simple CRUD operations inherited from {@link IGenericService}.
 *
 * <p>Provides transactional operations for user registration, verification, and search by email.
 * Error handling is included for invalid inputs and missing users.
 *
 * @see IGenericService
 * @see IUserService
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends GenericServiceImpl<User, UUID> implements IUserService {

    private final IUserDAO dao;

    /**
     * Provides the DAO component used by inherited service operations.
     *
     * @return the {@link IGenericDAO} implementation managing {@link User} persistence
     */
    @Override
    @Generated
    protected IGenericDAO<User, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a human-readable entity name used in exception messages.
     *
     * @return the name of the entity ("User")
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "User";
    }

    /**
     * Registers a new {@link User} in the system.
     *
     * @param user the user to register; must not be {@code null}
     * @return the saved {@link User} instance
     * @throws BadRequestException if the provided user is {@code null}
     */
    @Transactional
    @Override
    public User register(User user) {
        if (user == null)
            throw new BadRequestException(String.format("%s cannot be created", getEntityName()));
        return dao.save(user);
    }

    /**
     * Checks whether a user exists based on their email address.
     *
     * @param email the email to check; may be {@code null} or blank
     * @return {@code true} if a user with the specified email exists; {@code false} otherwise
     */
    @Transactional
    @Override
    public boolean existsByEmail(String email) {
        return dao.existsByEmail(email);
    }

    /**
     * Retrieves a {@link User} by their email address.
     *
     * @param email the email of the user to find; must not be {@code null} or blank
     * @return the found {@link User}
     * @throws BadRequestException if the email is {@code null} or blank
     * @throws NotFoundException if no user is found with the given email
     */
    @Transactional
    @Override
    public User findByEmail(String email) {
        if (email == null || email.isBlank())
            throw new BadRequestException(String.format("%s cannot be found", getEntityName()));
        return dao.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The email does not match any account"));
    }
}
