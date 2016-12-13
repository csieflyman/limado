/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.PartyDagEdge;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author csieflyman
 */
@Repository("partyDagEdgeDao")
class PartyDagEdgeDaoImpl extends DagEdgeDaoImpl<PartyDagEdge, UUID> {

}