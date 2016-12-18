/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.PartyIntervalTreeNode;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author csieflyman
 */
@Repository("partyIntervalTreeDao")
public class PartyIntervalTreeDaoImpl extends IntervalTreeDaoImpl<PartyIntervalTreeNode, UUID> {

    @Override
    protected String getTreeType() {
        return PartyIntervalTreeNode.TREE_TYPE;
    }
}
