package com.example.service.Impl;

import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import com.example.persistence.IGenericDAO;
import com.example.persistence.IUserDAO;
import com.example.qualifier.MainService;
import com.example.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@MainService
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends GenericServiceImpl<User, UUID> implements IUserService {

    private final IUserDAO dao;

    @Override
    protected IGenericDAO<User, UUID> getDAO() {
        return dao;
    }

    @Override
    protected String getEntityName() {
        return "User";
    }

    @Transactional
    @Override
    public User save(User user) {
        if(user == null) throw new BadRequestException(
                String.format("%s cannot be created", getEntityName()));
        if(super.existsByUniqueProperties(user))
            throw new BadRequestException("Email already registered");
        return dao.save(user);
    }

    @Transactional
    @Override
    public User register(User user) {
        if(user == null) throw new BadRequestException(
                String.format("%s cannot be created", getEntityName()));
        return dao.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return dao.existsByUsername(username);
    }

    @Transactional
    @Override
    public User findByUsername(String username) {
        if (username == null) throw new BadRequestException(
                String.format("%s cannot be found", getEntityName()));
        return dao.findByUsername(username).orElseThrow(() ->
                new NotFoundException("The email does not match any account"));
    }

}
