package com.alpaca.service.impl;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IUserService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Main Service for managing {@link User} entities. Extends {@link IGenericService} to inherit
 * common CRUD operations.
 *
 * @see IGenericService
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends GenericServiceImpl<User, UUID> implements IUserService {

    private final IUserDAO dao;

    @Override
    @Generated
    protected IGenericDAO<User, UUID> getDAO() {
        return dao;
    }

    @Override
    @Generated
    protected String getEntityName() {
        return "User";
    }

    @Transactional
    @Override
    public User register(User user) {
        if (user == null)
            throw new BadRequestException(String.format("%s cannot be created", getEntityName()));
        return dao.save(user);
    }

    @Transactional
    @Override
    public boolean existsByEmail(String email) {
        return dao.existsByEmail(email);
    }

    @Transactional
    @Override
    public User findByEmail(String email) {
        if (email == null || email.isBlank())
            throw new BadRequestException(String.format("%s cannot be found", getEntityName()));
        return dao.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The email does not match any account"));
    }
}
