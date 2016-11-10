/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.form;

import com.limado.collab.model.User;

/**
 * @author csieflyman
 */
public class UserForm extends PartyForm<User> {

    @Override
    public User buildModel() {
        User user = new User();
        populatePartyProperties(user);
        return user;
    }
}
