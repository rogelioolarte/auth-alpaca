package com.example.persistence.Impl;

import com.example.persistence.IGenericDAO;
import com.example.repository.GenericRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class GenericDAOImpl<T, ID> implements
        IGenericDAO<T, ID> {

    protected abstract GenericRepo<T, ID> getRepo();
    protected abstract EntityManager getEntityManager();
    protected abstract Class<T> getEntity();

    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable((T) getEntityManager().createNativeQuery(
                        "SELECT * FROM " + getEntity().getSimpleName().toLowerCase() + "s" +
                                " WHERE " + getEntity().getSimpleName().toLowerCase() +
                                "_id" + " = :id", getEntity())
                .setParameter("id", id).getResultList().stream().findFirst().orElse(null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> findAllByIds(Collection<ID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return (List<T>) getEntityManager().createNativeQuery("SELECT * FROM " +
                        getEntity().getSimpleName().toLowerCase() + "s" + " WHERE " +
                        getEntity().getSimpleName().toLowerCase() + "_id" + " IN :ids", getEntity())
                .setParameter("ids", ids).getResultList();
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
        if(t == null || t.isEmpty()) return List.of();
        return getRepo().saveAllAndFlush(t);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> findAll() {
        return (List<T>) getEntityManager().createNativeQuery("SELECT * FROM " +
                        getEntity().getSimpleName().toLowerCase() + "s", getEntity()).getResultList();
    }

    @Override
    public Page<T> findAllPage(Pageable pageable) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(getEntity());
        Root<T> root = cq.from(getEntity());
        if (pageable.getSort().isSorted()) {
            List<Order> orders = pageable.getSort().stream()
                    .map(order -> order.isAscending() ?
                            cb.asc(root.get(order.getProperty())) :
                            cb.desc(root.get(order.getProperty())))
                    .toList();
            cq.orderBy(orders);
        }
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(countQuery.from(getEntity())));
        return new PageImpl<>(getEntityManager().createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList(), pageable,
                getEntityManager().createQuery(countQuery).getSingleResult());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean existsById(ID id) {
        if (id == null) return false;
        return ((Number) getEntityManager().createNativeQuery("SELECT COUNT(*) FROM " +
                        getEntity().getSimpleName().toLowerCase() + "s" + " WHERE id = :id")
                .setParameter("id", id)
                .getResultList().stream().findFirst().orElse(0)).intValue() > 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean existsAllById(Collection<ID> ids) {
        if (ids == null || ids.isEmpty()) return false;
        return ((Number) getEntityManager().createNativeQuery("SELECT COUNT(*) FROM " +
                        getEntity().getSimpleName().toLowerCase() + "s" + " WHERE id IN :ids")
                .setParameter("ids", ids)
                .getResultList().stream().findFirst().orElse(0)).intValue() == ids.size();
    }

}
