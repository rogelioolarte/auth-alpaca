package com.example.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IGenericDAO<T, ID> {

    Optional<T> findById(ID id);
    List<T> findAllByIds(Collection<ID> ids);
    T updateById(T t, ID id);
    T save(T t);
    List<T> saveAll(List<T> t);
    void deleteById(ID id);
    List<T> findAll();
    Page<T> findAllPage(Pageable pageable);
    boolean existsById(ID id);
    boolean existsAllById(Collection<ID> ids);
    boolean existsByUniqueProperties(T t);
}
