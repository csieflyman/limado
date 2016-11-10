/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.Party;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * author flyman
 */
@Repository("partyDao")
public class PartyDaoImpl extends JpaGenericDaoImpl<Party, UUID> implements PartyDao {

    private static final Logger log = LogManager.getLogger(PartyDaoImpl.class);

    @Override
    public void addChild(Party parent, Party child) {
        parent.addChild(child);
        entityManager.merge(parent);
    }

    @Override
    public void removeChild(Party parent, Party child) {
        parent.removeChild(child);
        entityManager.merge(parent);
    }
}
