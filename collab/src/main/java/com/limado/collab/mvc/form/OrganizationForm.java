/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.form;

import com.limado.collab.model.Organization;

/**
 * @author csieflyman
 */
public class OrganizationForm extends PartyForm<Organization> {

    @Override
    public Organization buildModel() {
        Organization org = new Organization();
        populatePartyProperties(org);
        return org;
    }
}
