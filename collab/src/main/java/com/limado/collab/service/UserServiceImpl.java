package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.limado.collab.dao.IntervalTreeDao;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * author flyman
 */
@Service
public class UserServiceImpl extends PartyServiceImpl<User> implements UserService {

    @Autowired
    @Qualifier("partyIntervalTreeDao")
    private IntervalTreeDao<UUID> intervalTreeDao;

    @Override
    public void addChildren(User user, Set<Party> children) {
        if (children.isEmpty())
            return;
        throw new UnsupportedOperationException(String.format("user %s can't add children", user));
    }

    @Override
    public void removeChildren(User user, Set<Party> children) {
        if (children.isEmpty())
            return;
        throw new UnsupportedOperationException(String.format("user %s can't remove children", user));
    }

    @Override
    public void addChild(User user, Party child) {
        throw new UnsupportedOperationException(String.format("user %s can't add child", user));
    }

    @Override
    public void removeChild(User user, Party child) {
        throw new UnsupportedOperationException(String.format("user %s can't remove child", user));
    }

    @Override
    public void addParents(User child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if (parents.isEmpty())
            return;
        if (parents.stream().anyMatch(parent -> parent.getType().equals(User.TYPE))) {
            throw new IllegalArgumentException(String.format("user %s can't add user parent %s", child, parents));
        }
        if (parents.stream().filter(parent -> parent.getType().equals(Organization.TYPE)).count() > 1) {
            throw new IllegalArgumentException(String.format("user %s can't add above two organization parents %s", child, parents));
        }

        super.addParents(child, parents);
        for (Party parent : parents) {
            if (parent.getType().equals(Organization.TYPE)) {
                intervalTreeDao.addChild(parent.getId(), child.getId());
            }
        }
    }

    @Override
    public void removeParents(User child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if (parents.isEmpty())
            return;

        for (Party parent : parents) {
            if (parent.getType().equals(Organization.TYPE)) {
                intervalTreeDao.removeChild(parent.getId(), child.getId());
            }
        }
        super.removeParents(child, parents);
    }

    @Override
    public Set<Party> getChildren(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        return new HashSet<>();
    }

    @Override
    public Set<Party> getDescendants(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        return new HashSet<>();
    }

    @Override
    public void delete(User user) {
        Preconditions.checkArgument(user != null, "user must not be null");

        intervalTreeDao.delete(user.getId());
        super.delete(user);
    }
}
