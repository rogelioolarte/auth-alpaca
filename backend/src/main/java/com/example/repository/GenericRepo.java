package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

@NoRepositoryBean
public interface GenericRepo<T,ID> extends JpaRepository<T,ID> {

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.id IN :ids")
    long countByIds(@Param("ids") Collection<ID> ids);
}
