package com.indra.retail.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones CRUD genéricas usando directamente el {@link EntityManager}.
 *
 * Cada método abre su propia transacción: al terminar, el contexto de
 * persistencia se cierra y las entidades devueltas quedan DETACHED. Por eso
 * acceder a una colección LAZY (p. ej. Order.getItems()) con el resultado de
 * {@link #findById} lanza LazyInitializationException.
 */
@Repository
public class EntityManagerRecordManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public <T> T create(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Transactional
    public <T> T update(T entity) {
        return entityManager.merge(entity);
    }

    @Transactional
    public <T> boolean delete(Class<T> type, Object id) {
        T entity = entityManager.find(type, id);
        if (entity == null) {
            return false;
        }
        entityManager.remove(entity);
        return true;
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> findById(Class<T> type, Object id) {
        return Optional.ofNullable(entityManager.find(type, id));
    }

    @Transactional(readOnly = true)
    public <T> List<T> findAll(Class<T> type) {
        CriteriaQuery<T> query = entityManager.getCriteriaBuilder().createQuery(type);
        query.select(query.from(type));
        return entityManager.createQuery(query).getResultList();
    }
}
