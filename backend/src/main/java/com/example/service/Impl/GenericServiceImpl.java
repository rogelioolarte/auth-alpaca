package com.example.service.impl;

import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import com.example.persistence.IGenericDAO;
import com.example.service.IGenericService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Abstract service implementation providing common CRUD operations.
 * This class serves as a base for entity-specific service implementations.
 *
 * @param <T>  The type of entity.
 * @param <ID> The type of the entity's identifier.
 */
public abstract class GenericServiceImpl<T, ID> implements IGenericService<T, ID> {

    /**
     * Gets the Data Access Object (DAO) associated with the entity.
     *
     * @return The DAO instance.
     */
    protected abstract IGenericDAO<T, ID> getDAO();

    /**
     * Gets the name of the entity for error messages.
     *
     * @return The entity name.
     */
    protected abstract String getEntityName();

    @Transactional
    @Override
    public T findById(ID id) {
        if (id == null) throw new BadRequestException(
                String.format("%s cannot be found", getEntityName()));
        return getDAO().findById(id).orElseThrow(() -> new NotFoundException(
                String.format("%s with ID %s not found", getEntityName(), id)));
    }

    @Transactional
    @Override
    public List<T> findAllByIds(Collection<ID> ids) {
        if (ids == null || ids.isEmpty() || ids.contains(null))
            throw new BadRequestException(
                    String.format("%s(s) cannot be found", getEntityName()));
        if (!existsAllByIds(ids)) throw new NotFoundException(
                String.format("Some %s(s) cannot be found", getEntityName()));
        return getDAO().findAllByIds(ids);
    }

    public Set<T> findAllByIdsToSet(Collection<ID> ids) {
        return new HashSet<>(findAllByIds(ids));
    }

    @Transactional
    @Override
    public T save(T t) {
        if (t == null) throw new BadRequestException(
                String.format("%s cannot be created", getEntityName()));
        if (existsByUniqueProperties(t)) throw new BadRequestException(
                        String.format("%s already exists", getEntityName()));
        return getDAO().save(t);
    }

    @Transactional
    @Override
    public List<T> saveAll(Collection<T> t) {
        if (t == null || t.isEmpty() || t.contains(null))
            throw new BadRequestException(String.format("%s cannot be created", getEntityName()));
        return getDAO().saveAll(t);
    }

    @Transactional
    @Override
    public T updateById(T t, ID id) {
        if (id == null || t == null) throw new BadRequestException(
                String.format("%s cannot be updated", getEntityName()));
        return Optional.ofNullable(getDAO().updateById(t, id)).orElseThrow(() ->
                new BadRequestException(String.format("%s with ID %s cannot be updated",
                        getEntityName(), id.toString())));
    }

    @Transactional
    @Override
    public void deleteById(ID id) {
        if (id == null) throw new BadRequestException(
                String.format("%s cannot be deleted", getEntityName()));
        if (!existsById(id)) throw new BadRequestException(
                String.format("%s not exists", getEntityName()));
        getDAO().deleteById(id);
    }

    @Transactional
    @Override
    public List<T> findAll() {
        return getDAO().findAll();
    }

    @Transactional
    @Override
    public Page<T> findAllPage(Pageable pageable) {
        if (pageable == null) throw new BadRequestException(
                String.format("%s(s) Page cannot be created", getEntityName()));
        return getDAO().findAllPage(pageable);
    }

    @Transactional
    @Override
    public boolean existsById(ID id) {
        if (id == null) return false;
        return getDAO().existsById(id);
    }

    @Transactional
    @Override
    public boolean existsAllByIds(Collection<ID> ids) {
        if (ids == null || ids.isEmpty() || ids.contains(null)) return false;
        return getDAO().existsAllByIds(ids);
    }

    @Transactional
    @Override
    public boolean existsByUniqueProperties(T t) {
        if (t == null) return false;
        return getDAO().existsByUniqueProperties(t);
    }
}
