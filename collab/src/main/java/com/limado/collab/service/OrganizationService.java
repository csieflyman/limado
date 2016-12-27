package com.limado.collab.service;

import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;

import java.util.UUID;

/**
 * author flyman
 */
public interface OrganizationService extends PartyService<Organization> {

    void movePartyToOrganization(Party child, Organization organization);
}
