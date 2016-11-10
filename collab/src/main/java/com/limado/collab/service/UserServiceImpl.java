/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * author flyman
 */
@Service
public class UserServiceImpl extends PartyServiceImpl<User> implements UserService {

    @Override
    public void addChild(UUID parentId, UUID childId) {
        throw new UnsupportedOperationException(String.format("user %s can't add child %s", parentId, childId));
    }

    @Override
    public void removeChild(UUID parentId, UUID childId) {
        throw new UnsupportedOperationException(String.format("user %s can't remove child %s", parentId, childId));
    }

    @Override
    public Set<Party> getChildren(UUID userId) {
        throw new UnsupportedOperationException(String.format("user %s can't get children", userId));
    }
}
