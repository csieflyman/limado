package com.limado.collab.dao;

import com.limado.collab.model.PartyDagEdge;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author csieflyman
 */
@Repository("partyDagEdgeDao")
class PartyDagEdgeDaoImpl extends DagEdgeDaoImpl<PartyDagEdge, UUID> {

    @Override
    protected String getDagId() {
        return PartyDagEdge.DAG_ID;
    }
}
