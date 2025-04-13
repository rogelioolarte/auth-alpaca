package com.alpaca.persistence.impl;

import com.alpaca.persistence.IGenericDAO;
import com.alpaca.repository.GenericRepo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public abstract class GenericDAOImpl<T, ID> implements IGenericDAO<T, ID> {

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
    return getRepo().save(t);
  }

  @Override
  public List<T> saveAll(Collection<T> t) {
    return getRepo().saveAll(t);
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
    return ((long) ids.size()) == getRepo().countByIds(ids);
  }
}
