package com.limado.collab.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.limado.collab.model.Party;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceUnitUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Repository("partyDao")
public class PartyDaoImpl extends JpaGenericDaoImpl<Party, UUID> implements PartyDao {

    private static final Logger log = LogManager.getLogger(PartyDaoImpl.class);

    @Override
    public void addChild(Party parent, Party child) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(child != null, "child must not be null");

        parent = loadParent(parent);
        child = loadChild(child);
        if (parent.getChildren().contains(child)) {
            throw new IllegalArgumentException(String.format("%s is already a child of %s", child, parent));
        }
        parent.addChild(child);
        entityManager.merge(parent);
    }

    @Override
    public void removeChild(Party parent, Party child) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(child != null, "child must not be null");

        parent = loadParent(parent);
        child = loadChild(child);
        if (!parent.getChildren().contains(child)) {
            throw new IllegalArgumentException(String.format("%s is not a child of %s", child, parent));
        }
        parent.removeChild(child);
        entityManager.merge(parent);
    }

    @Override
    public void addChildren(Party parent, Set<Party> children) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(children != null, "children must not be null");

        if (children.isEmpty())
            return;

        parent = loadParent(parent);
        children = loadChildren(children);
        if (CollectionUtils.containsAny(parent.getChildren(), children)) {
            throw new IllegalArgumentException(String.format("%s already contains some children %s", parent, children));
        }
        children = new HashSet<>(children);
        for (Party child : children) {
            parent.addChild(child);
        }
        entityManager.merge(parent);
    }

    @Override
    public void removeChildren(Party parent, Set<Party> children) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(children != null, "children must not be null");

        if (children.isEmpty())
            return;

        parent = loadParent(parent);
        children = loadChildren(children);
        if (!CollectionUtils.isSubCollection(children, parent.getChildren())) {
            throw new IllegalArgumentException(String.format("%s doesn't contains some children %s", parent, children));
        }
        children = new HashSet<>(children);
        for (Party child : children) {
            parent.removeChild(child);
        }
        entityManager.merge(parent);
    }

    @Override
    public void addParents(Party child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if (parents.isEmpty())
            return;

        child = loadChild(child);
        parents = loadParents(parents);
        for (Party parent : parents) {
            if (parent.getChildren().contains(child)) {
                throw new IllegalArgumentException(String.format("%s is already a child of %s", child, parent));
            }
            parent.addChild(child);
            entityManager.merge(parent);
        }
    }

    @Override
    public void removeParents(Party child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if (parents.isEmpty())
            return;

        child = loadChild(child);
        parents = loadParents(parents);
        for (Party parent : parents) {
            if (!parent.getChildren().contains(child)) {
                throw new IllegalArgumentException(String.format("%s is not a child of %s", child, parent));
            }
            parent.removeChild(child);
            entityManager.merge(parent);
        }
    }

    private Party loadParent(Party parent) {
        if (isLoadedParent(parent))
            return parent;

        UUID parentId = parent.getId();
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.EQ, parentId));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_CHILDREN));
        List<Party> parties = find(params);
        parent = parties.isEmpty() ? null : parties.get(0);

        if (parent == null) {
            throw new IllegalArgumentException(String.format("party %s is not exist", parentId));
        }
        return parent;
    }

    private Set<Party> loadParents(Set<Party> parents) {
        if (parents.stream().allMatch(this::isLoadedChild))
            return parents;

        Set<UUID> parentsIds = parents.stream().map(Party::getId).collect(Collectors.toSet());
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, parentsIds));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_CHILDREN));
        parents = new HashSet<>(find(params));
        if (parents.size() != parentsIds.size()) {
            Set<UUID> foundParentsIds = parents.stream().map(Party::getId).collect(Collectors.toSet());
            throw new IllegalArgumentException(String.format("parents id %s are not exist", CollectionUtils.subtract(parentsIds, foundParentsIds)));
        }
        return parents;
    }

    private Party loadChild(Party child) {
        if (isLoadedChild(child))
            return child;

        UUID childId = child.getId();
        child = getById(childId);
        if (child == null) {
            throw new IllegalArgumentException(String.format("party %s is not exist", childId));
        }
        return child;
    }

    private Set<Party> loadChildren(Set<Party> children) {
        if (children.stream().allMatch(this::isLoadedChild))
            return children;

        Set<UUID> childrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, childrenIds));
        children = new HashSet<>(find(params));
        if (children.size() != childrenIds.size()) {
            Set<UUID> foundChildrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
            throw new IllegalArgumentException(String.format("children id %s are not exist", CollectionUtils.subtract(childrenIds, foundChildrenIds)));
        }
        return children;
    }

    private boolean isLoadedParent(Party parent) {
        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        return entityManager.contains(parent) && util.isLoaded(parent) && util.isLoaded(parent, Party.RELATION_CHILDREN);
    }

    private boolean isLoadedChild(Party child) {
        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        return entityManager.contains(child) && util.isLoaded(child);
    }
}