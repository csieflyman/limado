/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;



import com.limado.collab.model.Identifiable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * @author csieflyman
 */
interface BatchProcessingDao<T extends Identifiable, ID extends Serializable> {

    void batchCreate(Collection<T> entities);

    void batchUpdate(Collection<T> entities);

    void batchUpdate(Collection<ID> ids, Map<String, Object> updatedValueMap);

    void batchDelete(Collection<T> entities);

    void batchDeleteById(Collection<ID> ids);
}