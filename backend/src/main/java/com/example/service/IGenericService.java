package com.example.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface IGenericService<T, ID> {

    T findById(ID id);
    List<T> findAllByIds(Collection<ID> ids);
    T save(T t);
    List<T> saveAll(List<T> t);
    T updateById(T t, ID id);
    void deleteById(ID id);
    List<T> findAll();
    Page<T> findAllPage(Pageable pageable);
    boolean existsById(ID id);
    boolean existsAllByIds(Collection<ID> ids);
    boolean existsByUniqueProperties(T t);
}
