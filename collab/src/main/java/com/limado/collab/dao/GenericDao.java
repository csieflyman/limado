package com.limado.collab.dao;

import com.limado.collab.model.Identifiable;
import com.limado.collab.util.query.QueryParams;

import java.io.Serializable;
import java.util.List;

/**
 * author flyman
 */
interface GenericDao<T extends Identifiable<ID>, ID extends Serializable> {

    T create(T entity);

    void update(T entity);

    void delete(T entity);

    T getById(ID id);

    List<T> find(QueryParams queryParams);

    int findSize(QueryParams queryParams);
}
