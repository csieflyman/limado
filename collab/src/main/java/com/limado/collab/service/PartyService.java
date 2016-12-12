/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.limado.collab.model.Party;
import com.limado.collab.util.query.QueryParams;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * author flyman
 */
public interface PartyService<T extends Party> {

    T create(T party);

    void update(T party);

    T get(T party, String... relations);

    T get(T party, Collection<String> relations);

    Party getById(UUID id, String... relations);

    Party getById(UUID id, Collection<String> relations);

    boolean checkExist(String type, String identity);

    void deleteById(UUID id);

    void deleteByIds(Collection<UUID> ids);

    List<Party> find(QueryParams queryParams);

    int findSize(QueryParams queryParams);

    void enable(Collection<UUID> ids);

    void disable(Collection<UUID> ids);

    Set<Party> getParents(UUID id);

    Set<Party> getChildren(UUID id);

    Set<Party> getAscendants(UUID id);

    Set<Party> getDescendants(UUID id);

    void addChild(UUID parentId, UUID childId);

    void removeChild(UUID parentId, UUID childId);

    void addChildren(UUID parentId, Set<UUID> childrenIds);

    void removeChildren(UUID parentId, Set<UUID> childrenIds);

    void addParents(UUID childId, Set<UUID> parentsIds);

    void removeParents(UUID childId, Set<UUID> parentsIds);
}
