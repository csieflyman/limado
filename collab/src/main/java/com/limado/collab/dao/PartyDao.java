/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.Party;

import java.util.Set;
import java.util.UUID;

/**
 * author flyman
 */
public interface PartyDao extends GenericDao<Party, UUID>, BatchProcessingDao<Party, UUID>{

    void addChild(UUID parent, UUID child);

    void removeChild(UUID parent, UUID child);

    void addChildren(UUID parent, Set<UUID> children);

    void removeChildren(UUID parent, Set<UUID> children);

    void addParents(UUID child, Set<UUID> parents);

    void removeParents(UUID child, Set<UUID> parents);
}
