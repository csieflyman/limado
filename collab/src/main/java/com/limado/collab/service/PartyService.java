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

    void delete(T party);

    List<Party> find(QueryParams queryParams);

    int findSize(QueryParams queryParams);

    void enable(Collection<UUID> ids);

    void disable(Collection<UUID> ids);

    Set<Party> getParents(UUID id);

    Set<Party> getChildren(UUID id);

    Set<Party> getAscendants(UUID id);

    Set<Party> getDescendants(UUID id);

    void addChild(T parent, Party child);

    void removeChild(T parent, Party child);

    void addChildren(T parent, Set<Party> children);

    void removeChildren(T parent, Set<Party> children);

    void addParents(T child, Set<Party> parents);

    void removeParents(T child, Set<Party> parents);
}
