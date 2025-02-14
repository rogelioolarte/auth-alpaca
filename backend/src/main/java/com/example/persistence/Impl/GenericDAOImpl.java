package com.example.persistence.impl;

import com.example.persistence.IGenericDAO;
import com.example.repository.GenericRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class GenericDAOImpl<T, ID> implements
        IGenericDAO<T, ID> {

    protected abstract GenericRepo<T, ID> getRepo();
    protected abstract Class<T> getEntity();

    @Override
    public Optional<T> findById(ID id) {
        return getRepo().findById(id);
    }

    @Override
    public List<T> findAllByIds(Collection<ID> ids) {
        return getRepo().findAllById(ids);
    }

    @Override
    public void deleteById(ID id) {
        getRepo().deleteById(id);
    }

    @Override
    public T save(T t) {
        return getRepo().saveAndFlush(t);
    }

    @Override
    public List<T> saveAll(List<T> t) {
        return getRepo().saveAllAndFlush(t);
    }

    @Override
    public List<T> findAll() {
        return getRepo().findAll();
    }

    @Override
    public Page<T> findAllPage(Pageable pageable) {
        return getRepo().findAll(pageable);
    }

    @Override
    public boolean existsById(ID id) {
        return getRepo().existsById(id);
    }

    @Override
    public boolean existsAllByIds(Collection<ID> ids) {
        return getRepo().countByIds(ids) ==  (long) ids.size();
    }

}
