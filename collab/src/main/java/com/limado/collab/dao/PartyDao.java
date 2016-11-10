/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.Party;

import java.util.UUID;

/**
 * author flyman
 */
public interface PartyDao extends GenericDao<Party, UUID>, BatchProcessingDao<Party, UUID>{

    void addChild(Party parent, Party child);

    void removeChild(Party parent, Party child);
}
