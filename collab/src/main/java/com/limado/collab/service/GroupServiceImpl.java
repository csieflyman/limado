package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * author flyman
 */
@Service
public class GroupServiceImpl extends PartyServiceImpl<Group> implements GroupService {

    @Override
    public void addParents(Group child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if (parents.isEmpty())
            return;
        if (parents.stream().anyMatch(parent -> parent.getType().equals(User.TYPE) || parent.getType().equals(Organization.TYPE))) {
            throw new IllegalArgumentException(String.format("group %s can't add user or organization parent %s", child, parents));
        }

        super.addParents(child, parents);
    }
}
