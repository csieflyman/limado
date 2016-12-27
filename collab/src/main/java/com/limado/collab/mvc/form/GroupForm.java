package com.limado.collab.mvc.form;

import com.limado.collab.model.Group;

/**
 * @author csieflyman
 */
public class GroupForm extends PartyForm<Group> {

    @Override
    public Group buildModel() {
        Group group = new Group();
        populatePartyProperties(group);
        return group;
    }
}
