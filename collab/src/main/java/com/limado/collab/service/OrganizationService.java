/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.limado.collab.model.Organization;

import java.util.UUID;

/**
 * author flyman
 */
public interface OrganizationService extends PartyService<Organization>{

    void movePartyToOrganization(UUID partyId, UUID organizationId);
}
