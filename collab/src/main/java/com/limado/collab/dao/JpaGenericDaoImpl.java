/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.Identifiable;
import com.limado.collab.util.query.QueryParams;
import com.limado.collab.util.query.QueryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * author flyman
 */
abstract class JpaGenericDaoImpl<T extends Identifiable<ID>, ID extends Serializable>
        implements GenericDao<T, ID>, BatchProcessingDao<T, ID>{

    private static final Logger log = LogManager.getLogger(JpaGenericDaoImpl.class);

    @PersistenceContext
    EntityManager entityManager;

    private int batchSize = 20;

    protected Class<T> clazz;

    JpaGenericDaoImpl() {
        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public T create(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Override
    public void update(T entity) {
        entityManager.merge(entity);
    }

    @Override
    public void delete(T entity) {
        entityManager.remove(entity);
    }

    @Override
    public T getById(ID id) {
        return entityManager.find(clazz, id);
    }

    @Override
    public List<T> find(QueryParams queryParams) {
        StringBuilder sb = new StringBuilder();
        StringBuilder selectClause = new StringBuilder();
        StringBuilder fromClause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();
        StringBuilder orderByClause = new StringBuilder();

        EntityType entityType = entityManager.getMetamodel().entity(clazz);
        String entityAlias = QueryUtils.getEntityAlias(entityType);

        selectClause.append("select ");
        fromClause.append("from ").append(entityType.getName()).append(" ").append(entityAlias).append(" ");

        Set<String> fetchRelations = queryParams.getFetchRelations();
        if(!fetchRelations.isEmpty()) {
            boolean distinct = false;
            for (String relation: fetchRelations) {
                if(entityType.getAttribute(relation).isCollection()) {
                    distinct = true;
                    fromClause.append("left join fetch ");
                }
                else {
                    fromClause.append("inner join fetch ");
                }
                fromClause.append(entityAlias).append(".").append(relation).append(" ").append(relation).append(" ");
            }
            if(distinct) {
                selectClause.append("distinct ");
            }
        }

        Set<String> predicateRelations = queryParams.getPredicateRelations();
        for(String relation: predicateRelations) {
            if(relation.equalsIgnoreCase(entityAlias) || fetchRelations.contains(relation))
                continue;

            if(entityType.getAttribute(relation).isCollection()) {
                fromClause.append("left join ");
            }
            else {
                fromClause.append("inner join ");
            }
            fromClause.append(entityAlias).append(".").append(relation).append(" ").append(relation).append(" ");
        }

        selectClause.append(entityAlias).append(" ");

        if(!queryParams.getPredicates().isEmpty()) {
            whereClause.append("where ").append(QueryUtils.buildHqlWhereClause(entityType,
                    queryParams.getPredicates(), queryParams.isPredicatesDisjunction())).append(" ");
        }
        if(!queryParams.getOrderByList().isEmpty()) {
            orderByClause.append(QueryUtils.buildHqlOrderByClause(entityType, queryParams.getOrderByList()));
        }

        sb.append(selectClause.toString()).append(fromClause.toString()).append(whereClause.toString()).append(orderByClause);
        log.debug(sb.toString());
        Query query = entityManager.createQuery(sb.toString());
        QueryUtils.setQueryParameterValue(query, queryParams.getPredicates());

        if(queryParams.getOffset() >= 0) {
            query.setFirstResult(queryParams.getOffset());
            query.setMaxResults(queryParams.getLimit());
        }

        List<T> result = query.getResultList();
        log.debug(result.size());
        return result;
    }

    @Override
    public int findSize(QueryParams queryParams) {
        StringBuilder sb = new StringBuilder();
        StringBuilder selectClause = new StringBuilder();
        StringBuilder fromClause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();
        StringBuilder orderByClause = new StringBuilder();

        EntityType entityType = entityManager.getMetamodel().entity(clazz);
        String entityAlias = QueryUtils.getEntityAlias(entityType);

        selectClause.append("select count(*) ");
        fromClause.append("from ").append(entityType.getName()).append(" ").append(entityAlias).append(" ");

        Set<String> predicateRelations = queryParams.getPredicateRelations();
        for(String relation: predicateRelations) {
            if(relation.equalsIgnoreCase(entityAlias))
                continue;
            fromClause.append("left join ").append(entityAlias).append(".").append(relation).append(" ").append(relation).append(" ");
        }

        if(!queryParams.getPredicates().isEmpty()) {
            whereClause.append("where ").append(QueryUtils.buildHqlWhereClause(entityType,
                    queryParams.getPredicates(), queryParams.isPredicatesDisjunction())).append(" ");
        }

        sb.append(selectClause.toString()).append(fromClause.toString()).append(whereClause.toString()).append(orderByClause);
        log.debug(sb.toString());
        Query query = entityManager.createQuery(sb.toString());
        QueryUtils.setQueryParameterValue(query, queryParams.getPredicates());
        Long size = (Long) query.getSingleResult();
        log.debug("size = " + size);
        return size.intValue();
    }

    @Override
    public void batchCreate(Collection<T> entities) {
        if(entities.size() == 0)
            return;

        int count = 0;
        for(T entity: entities) {
            create(entity);
            if(++count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    @Override
    public void batchUpdate(Collection<T> entities) {
        if(entities.size() == 0)
            return;

        int count = 0;
        for(T entity: entities) {
            update(entity);
            if(++count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    @Override
    public void batchUpdate(Collection<ID> ids, Map<String, Object> updatedValueMap) {
        if(ids.size() == 0)
            return;

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for(String key: updatedValueMap.keySet()) {
            sb.append(key).append(" = :").append(key);
            count++;
            if(count != updatedValueMap.size())
                sb.append(", ");
        }

        String updateHQL = "update " + clazz.getSimpleName() + " set " + sb.toString() + " where id in (:ids)";
        int effectRows = batchExecute(updateHQL, ids, updatedValueMap);
        log.debug(updateHQL + "; effectRows = " + effectRows);
    }

    @Override
    public void batchDelete(Collection<T> entities) {
        if(entities.size() == 0)
            return;

        int count = 0;
        for(T entity: entities) {
            delete(entity);
            if(++count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    @Override
    public void batchDeleteById(Collection<ID> ids) {
        if(ids.size() == 0)
            return;

        String deleteHQL = "delete " + clazz.getSimpleName() + " where id in (:ids)";
        int effectRows = batchExecute(deleteHQL, ids, null);
        log.debug(deleteHQL + "; effectRows = " + effectRows);
    }

    private int batchExecute(String hql, Collection<ID> ids, Map<String, Object> parameterMap) {
        int effectRows = 0;
        int index = 0;
        Set<ID> batchIdSet = new HashSet<>();
        for(Iterator<ID> i = ids.iterator(); i.hasNext();) {
            batchIdSet.add(i.next());
            index++;
            if(index == batchSize) {
                Query query = entityManager.createQuery(hql);
                query.setParameter("ids", batchIdSet);
                if(parameterMap != null) {
                    for(Map.Entry<String, Object> entry: parameterMap.entrySet()) {
                        query.setParameter(entry.getKey(), entry.getValue());
                    }
                }
                effectRows += query.executeUpdate();
                index = 0;
                batchIdSet.clear();
            }
        }
        if(index > 0) {
            Query query = entityManager.createQuery(hql);
            query.setParameter("ids", batchIdSet);
            if(parameterMap != null) {
                for(Map.Entry<String, Object> entry: parameterMap.entrySet()) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }
            effectRows += query.executeUpdate();
        }
        return effectRows;
    }
}